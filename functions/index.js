/* eslint-disable max-len, no-trailing-spaces */
const {setGlobalOptions} = require("firebase-functions/v2");
const {onRequest} = require("firebase-functions/v2/https");
const admin = require("firebase-admin");

const {
  callGrok,
  generateImageAI,
} = require("./services/grokService");

const elevenService = require("./services/elevenService");

if (!admin.apps.length) {
  admin.initializeApp();
}

setGlobalOptions({cpu: "gcf_gen1"});

const db = admin.firestore();

/**
 * Verifies the Firebase auth token from the incoming request.
 * @param {Object} req Incoming HTTP request.
 * @return {Promise<string>} Firebase user ID.
 */
async function verifyUser(req) {
  const authHeader = req.headers.authorization || "";
  const token = authHeader.replace("Bearer ", "").trim();
  if (!token) throw new Error("Unauthorized");
  const decoded = await admin.auth().verifyIdToken(token);
  return decoded.uid;
}

/**
 * Resets daily usage counters when the date changes.
 * @param {FirebaseFirestore.DocumentReference} userRef User document ref.
 * @param {Object} user User document data.
 * @return {Promise<void>}
 */
async function resetDailyIfNeeded(userRef, user) {
  const now = Date.now();
  const lastReset = user.lastResetDate || 0;
  if (new Date(now).toDateString() !== new Date(lastReset).toDateString()) {
    const updateData = {dailyMessageCount: 0, dailyImageCount: 0, dailyVoiceCount: 0, lastResetDate: now};
    await userRef.update(updateData);
    user.dailyMessageCount = 0; 
    user.dailyImageCount = 0;
    user.dailyVoiceCount = 0;
    user.lastResetDate = now;
  }
}

/**
 * Downgrades expired subscriptions.
 * @param {FirebaseFirestore.DocumentReference} userRef User document ref.
 * @param {Object} user User document data.
 * @return {Promise<void>}
 */
async function checkSubscription(userRef, user) {
  if (user.subscriptionEnd && Date.now() > user.subscriptionEnd) {
    await userRef.update({plan: "free"});
    user.plan = "free";
  }
}

/**
 * Gets the daily message limit for a plan.
 * @param {string} plan Subscription plan name.
 * @return {number} Message limit.
 */
function getDailyMessageLimit(plan) {
  if (plan === "premium") return 25;
  if (plan === "ultra") return 75;
  return 5;
}

exports.generateImage = onRequest({
  secrets: ["XAI_API_KEY", "GROK_API_KEY"],
  timeoutSeconds: 120,
}, async (req, res) => {
  try {
    const userId = await verifyUser(req);
    const {prompt} = req.body || {};
    
    if (!prompt) return res.status(400).send("Missing prompt");

    const userRef = db.collection("users").doc(userId);
    const userSnap = await userRef.get();
    if (!userSnap.exists) return res.status(404).send("User not found");
    const user = userSnap.data();

    await resetDailyIfNeeded(userRef, user);
    await checkSubscription(userRef, user);

    const imageBase64 = await generateImageAI(prompt);
    
    await userRef.update({
      dailyImageCount: admin.firestore.FieldValue.increment(1),
    });

    await db.collection("usage").add({
      userId, 
      type: "image", 
      prompt: prompt.substring(0, 500),
      hasImage: !!imageBase64,
      createdAt: Date.now(),
    });

    return res.json({imageBase64});
  } catch (e) {
    console.error("GENERATE IMAGE ERROR:", e);
    const errorMsg = e.message || "Image generation failed";
    return res.status(500).send(errorMsg);
  }
});

exports.chatWithBot = onRequest({
  secrets: ["XAI_API_KEY", "GROK_API_KEY"],
  timeoutSeconds: 60,
}, async (req, res) => {
  try {
    const userId = await verifyUser(req);
    const {botId, message, systemPrompt, history} = req.body || {};
    
    if (!userId || !botId || !message) return res.status(400).send("Missing parameters");

    const userRef = db.collection("users").doc(userId);
    const userSnap = await userRef.get();
    const user = userSnap.exists ? userSnap.data() : {plan: "free", credits: 50};

    await resetDailyIfNeeded(userRef, user);
    await checkSubscription(userRef, user);

    const limit = getDailyMessageLimit(user.plan || "free");
    const currentCount = user.dailyMessageCount || 0;
    const currentCredits = user.credits || 0;

    if (currentCount >= limit && currentCredits < 1) {
      return res.status(403).send("Limit reached. Upgrade for more chats!");
    }

    const botSnap = await db.collection("bots").doc(botId).get();
    if (!botSnap.exists) return res.status(404).send("Bot not found");
    const bot = botSnap.data();

    const messages = [];
    messages.push({role: "system", content: systemPrompt || bot.systemPrompt || "You are a helpful assistant."});
    if (Array.isArray(history)) {
      history.forEach((h) => {
        if (h.role && h.text) messages.push({role: h.role, content: h.text});
      });
    }
    messages.push({role: "user", content: message});

    const grokResponse = await callGrok(null, null, bot.model || "grok-4-1-fast-non-reasoning", 1000, messages);
    const aiReply = grokResponse.choices[0].message.content;

    const docRef = await db.collection("chats").add({
      userId, botId, role: "assistant", text: aiReply, createdAt: admin.firestore.FieldValue.serverTimestamp(),
    });

    const usageUpdates = {};
    if (currentCount < limit) {
      usageUpdates.dailyMessageCount = admin.firestore.FieldValue.increment(1);
    } else {
      usageUpdates.credits = admin.firestore.FieldValue.increment(-1);
    }
    await userRef.update(usageUpdates);

    return res.json({reply: aiReply, messageId: docRef.id});
  } catch (e) {
    return res.status(500).send(e.message);
  }
});

exports.verifyPurchase = onRequest({region: "us-central1"}, async (req, res) => {
  try {
    const userId = await verifyUser(req);
    const {sku, purchaseToken} = req.body || {};

    console.log(`Verifying purchase for user: ${userId}, SKU: ${sku}, Token: ${purchaseToken}`);

    const SKU_CONFIG = {
      "credits_100": {type: "credits", value: 100},
      "credits_360": {type: "credits", value: 360},
      "credits_700": {type: "credits", value: 700},
      "credits_1500": {type: "credits", value: 1500},
      "plan_premium": {type: "plan", value: "premium", credits: 100, duration: 30},
      "plan_ultra": {type: "plan", value: "ultra", credits: 200, duration: 30},
    };

    const config = SKU_CONFIG[sku];
    if (!config) {
      console.error(`Invalid SKU: ${sku}`);
      return res.status(400).json({success: false, message: "Invalid SKU"});
    }

    const userRef = db.collection("users").doc(userId);

    await db.runTransaction(async (transaction) => {
      const userSnap = await transaction.get(userRef);
      if (!userSnap.exists) {
        throw new Error("User document does not exist");
      }

      const updates = {};
      if (config.type === "credits") {
        updates.credits = admin.firestore.FieldValue.increment(config.value);
      } else {
        updates.plan = config.value;
        updates.credits = admin.firestore.FieldValue.increment(config.credits || 0);
        updates.subscriptionStart = Date.now();
        updates.subscriptionEnd = Date.now() + (config.duration * 24 * 60 * 60 * 1000);
      }

      transaction.update(userRef, updates);
    });

    console.log("Purchase verified and applied successfully");
    return res.json({success: true, message: "Purchase applied"});
  } catch (e) {
    console.error("VERIFY PURCHASE ERROR:", e);
    return res.status(500).json({success: false, message: e.message});
  }
});

exports.getVoices = onRequest({secrets: ["ELEVEN_API_KEY"]}, async (req, res) => {
  try {
    await verifyUser(req);
    const voices = await elevenService.getVoices();
    return res.json({voices});
  } catch (e) {
    return res.status(500).send(e.message);
  }
});

exports.textToSpeech = onRequest({secrets: ["ELEVEN_API_KEY"]}, async (req, res) => {
  try {
    const userId = await verifyUser(req);
    const {text, voiceId} = req.body || {};
    const userRef = db.collection("users").doc(userId);
    const userSnap = await userRef.get();
    const user = userSnap.data();

    if ((user.credits || 0) < 1) return res.status(403).send("Insufficient credits.");

    const audioBase64 = await elevenService.textToSpeech(text, voiceId);
    await userRef.update({credits: admin.firestore.FieldValue.increment(-1)});
    return res.json({audio: audioBase64});
  } catch (e) {
    return res.status(500).send(e.message);
  }
});

exports.rewardCredit = onRequest(async (req, res) => {
  try {
    const userId = await verifyUser(req);
    await db.collection("users").doc(userId).update({
      credits: admin.firestore.FieldValue.increment(20),
    });
    return res.json({success: true});
  } catch (e) {
    return res.status(500).send(e.message);
  }
});
