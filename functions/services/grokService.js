const axios = require("axios");
const {xai} = require("@ai-sdk/xai");
const {generateImage} = require("ai");

const BASE_URL = "https://api.x.ai/v1";

/**
 * Gets the configured xAI API key.
 * @return {string}
 */
function getApiKey() {
  const apiKey = process.env.GROK_API_KEY || process.env.XAI_API_KEY;

  if (!apiKey) {
    throw new Error("Missing xAI API key");
  }

  return apiKey;
}

/**
 * Builds request headers for xAI REST calls.
 * @return {{"Authorization": string, "Content-Type": string}}
 */
function getHeaders() {
  return {
    "Authorization": `Bearer ${getApiKey()}`,
    "Content-Type": "application/json",
  };
}

/**
 * Normalizes API errors into readable messages.
 * @param {*} error
 * @throws {Error}
 */
function rethrowApiError(error) {
  if (axios.isAxiosError(error)) {
    const msg =
      (error.response &&
        error.response.data &&
        ((error.response.data.error &&
          error.response.data.error.message) ||
          error.response.data.message)) ||
      error.message;

    throw new Error(msg);
  }

  throw error;
}

/**
 * Sends a chat request to Grok.
 * @param {string} systemPrompt
 * @param {string} userMessage
 * @param {string=} model
 * @param {number=} maxTokens
 * @return {Promise<object>}
 */
exports.callGrok = async (systemPrompt, userMessage, model, maxTokens) => {
  try {
    if (!userMessage && !systemPrompt) {
      throw new Error("Invalid message content");
    }

    // Restored the specific model you use: grok-4-1-fast-non-reasoning
    const targetModel = model || "grok-4-1-fast-non-reasoning";

    console.log(`Calling Grok API with model: ${targetModel}`);

    const response = await axios.post(
        `${BASE_URL}/chat/completions`,
        {
          model: targetModel,
          messages: [
            {
              role: "system",
              content: systemPrompt || "You are a helpful assistant.",
            },
            {
              role: "user",
              content: userMessage || "Hello",
            },
          ],
          max_tokens: maxTokens || 1000,
          temperature: 0.7,
        },
        {
          headers: getHeaders(),
          timeout: 60000,
        },
    );

    if (!response.data || !response.data.choices) {
      throw new Error("Invalid Grok response structure");
    }

    return response.data;
  } catch (error) {
    const errorDetails = error &&
      error.response &&
      error.response.data ?
      error.response.data :
      error.message;

    console.error("GROK API ERROR DETAILS:", JSON.stringify(errorDetails));
    rethrowApiError(error);
  }
};

/**
 * Generates an image with xAI SDK.
 * @param {string} prompt
 * @return {Promise<string>}
 */
exports.generateImageAI = async (prompt) => {
  try {
    const apiKey = process.env.XAI_API_KEY;

    if (!apiKey) {
      throw new Error("XAI_API_KEY not found");
    }

    const result = await generateImage({
      model: xai.image("grok-imagine-image"),
      prompt,
      apiKey: apiKey,
    });

    if (!result || !result.image || !result.image.base64) {
      throw new Error("Invalid image response");
    }

    return `data:image/png;base64,${result.image.base64}`;
  } catch (error) {
    console.error("IMAGE ERROR:", error);
    throw new Error("Image generation failed");
  }
};

/**
 * Placeholder for video generation.
 * @return {Promise<never>}
 */
exports.generateVideoAI = async () => {
  throw new Error("Video generation not supported by xAI yet");
};

/**
 * Placeholder for text to speech.
 * @return {Promise<never>}
 */
exports.textToSpeech = async () => {
  throw new Error("Text-to-speech not supported by xAI API");
};

/**
 * Placeholder for speech to text.
 * @return {Promise<never>}
 */
exports.speechToText = async () => {
  throw new Error("Speech-to-text not supported by xAI API");
};
