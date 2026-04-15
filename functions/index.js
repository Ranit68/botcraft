/* eslint-disable max-len, no-trailing-spaces */
const functions = require("firebase-functions");
const admin = require("firebase-admin");

const {
  callGrok,
  generateImageAI,
} = require("./services/grokService");

if (!admin.apps.length) {
  admin.initializeApp();
}

const db = admin.firestore();

async function verifyUser(req) {
  const authHeader = req.headers.authorization || "";
  const token = authHeader.replace("Bearer ", "").trim();
  if (!token) throw new Error("Unauthorized");
  const decoded = await admin.auth().verifyIdToken(token);
  return decoded.uid;
}

async function resetDailyIfNeeded(userRef, user) {
  const now = Date.now();
  if (new Date(now).toDateString() !== new Date(user.lastResetDate || 0).toDateString()) {
    await userRef.update({ dailyMessageCount: 0, dailyImageCount: 0, lastResetDate: now });
    user.dailyMessageCount = 0; user.dailyImageCount = 0;
  }
}

async function checkSubscription(userRef, user) {
  if (user.subscriptionEnd && Date.now() > user.subscriptionEnd) {
    await userRef.update({ plan: "free" });
    user.plan = "free";
  }
}

function getDailyMessageLimit(plan) {
  if (plan === "premium") return 25;
  if (plan === "ultra") return 75;
  return 5;
}

exports.verifyPurchase = functions.https.onRequest(async (req, res) => {
  try {
    const userId = await verifyUser(req);
    const {purchaseToken, sku} = req.body || {};
    const SKU_CONFIG = {
      "credits_100": {type: "credits", value: 100},
      "credits_360": {type: "credits", value: 360},
      "credits_700": {type: "credits", value: 700},
      "credits_1500": {type: "credits", value: 1500},
      "plan_premium": {type: "plan", value: "premium", credits: 100, duration: 30},
      "plan_ultra": {type: "plan", value: "ultra", credits: 200, duration: 30},
    };
    const config = SKU_CONFIG[sku];
    if (!config) return res.status(400).send("Invalid SKU");

    const userRef = db.collection("users").doc(userId);
    let updates = {};
    if (config.type === "credits") {
      updates.credits = admin.firestore.FieldValue.increment(config.value);
    } else {
      updates = { 
        plan: config.value, 
        credits: admin.firestore.FieldValue.increment(config.credits), 
        subscriptionEnd: Date.now() + (30*24*60*60*1000) 
      };
    }
    await userRef.update(updates);
    return res.json({success: true});
  } catch (e) { return res.status(500).send(e.message); }
});

exports.chatWithBot = functions.https.onRequest(async (req, res) => {
  try {
    const userId = await verifyUser(req);
    const {botId, message, systemPrompt} = req.body || {};
    const userRef = db.collection("users").doc(userId);
    const userSnap = await userRef.get();
    const user = userSnap.data();

    await checkSubscription(userRef, user);
    await resetDailyIfNeeded(userRef, user);

    const limit = getDailyMessageLimit(user.plan);
    if (user.dailyMessageCount >= limit && user.credits < 1) return res.status(403).send("Limit reached");

    const botSnap = await db.collection("bots").doc(botId).get();
    const bot = botSnap.data();
    const grokResponse = await callGrok(systemPrompt || bot.systemPrompt, message, bot.model || "grok-beta");
    const aiReply = grokResponse.choices[0].message.content;

    await db.collection("chats").add({
        userId, botId, role: "assistant", text: aiReply, createdAt: admin.firestore.FieldValue.serverTimestamp()
    });

    await userRef.update({ 
        dailyMessageCount: admin.firestore.FieldValue.increment(1),
        credits: user.dailyMessageCount >= limit ? admin.firestore.FieldValue.increment(-1) : admin.firestore.FieldValue.increment(0)
    });

    return res.json({reply: aiReply});
  } catch (e) { return res.status(500).send(e.message); }
});

exports.generateImage = functions.https.onRequest(async (req, res) => {
  try {
    const userId = await verifyUser(req);
    const {prompt} = req.body || {};
    const imageBase64 = await generateImageAI(prompt);
    await db.collection("usage").add({ userId, type: "image", prompt, imageBase64, createdAt: Date.now() });
    return res.json({imageBase64});
  } catch (e) { return res.status(500).send(e.message); }
});

exports.rewardCredit = functions.https.onRequest(async (req, res) => {
  try {
    const userId = await verifyUser(req);
    await db.collection("users").doc(userId).update({
      credits: admin.firestore.FieldValue.increment(20)
    });
    return res.json({success: true});
  } catch (e) { return res.status(500).send(e.message); }
});
