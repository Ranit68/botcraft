const axios = require("axios");

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
 * Supports both legacy systemPrompt/userMessage and full conversation
 * history.
 * @param {string|null} systemPrompt
 * @param {string|null} userMessage
 * @param {string=} model
 * @param {number=} maxTokens
 * @param {Array=} messageList - Full conversation array
 * @return {Promise<object>}
 */
exports.callGrok = async (
    systemPrompt,
    userMessage,
    model,
    maxTokens,
    messageList,
) => {
  try {
    const targetModel = model || "grok-4-1-fast-non-reasoning";

    // Determine the messages array to send
    let finalMessages = [];
    if (Array.isArray(messageList) && messageList.length > 0) {
      finalMessages = messageList;
    } else {
      if (!userMessage && !systemPrompt) {
        throw new Error(
            "Invalid message content: both message and prompt are missing",
        );
      }
      finalMessages = [
        {
          role: "system",
          content: systemPrompt || "You are a helpful assistant.",
        },
        {
          role: "user",
          content: userMessage || "Hello",
        },
      ];
    }

    console.log(
        `Calling Grok API with model: ${targetModel}. ` +
        `Message count: ${finalMessages.length}`,
    );

    const response = await axios.post(
        `${BASE_URL}/chat/completions`,
        {
          model: targetModel,
          messages: finalMessages,
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
 * Generates an image using xAI REST API directly.
 * @param {string} prompt
 * @return {Promise<string>}
 */
exports.generateImageAI = async (prompt) => {
  try {
    const apiKey = process.env.XAI_API_KEY || process.env.GROK_API_KEY;

    if (!apiKey) {
      throw new Error("API Key (XAI_API_KEY or GROK_API_KEY) not found");
    }

    console.log(
        "Generating image with prompt:",
        prompt.substring(0, 100) + "...",
    );

    const response = await axios.post(
        "https://api.x.ai/v1/images/generations",
        {
          model: "grok-imagine-image-quality",
          prompt: prompt,
          n: 1,
          response_format: "b64_json",
        },
        {
          headers: {
            "Authorization": `Bearer ${apiKey}`,
            "Content-Type": "application/json",
          },
          timeout: 110000,
        },
    );

    if (
      response.data &&
      response.data.data &&
      response.data.data[0] &&
      response.data.data[0].b64_json
    ) {
      const b64 = response.data.data[0].b64_json;
      return `data:image/png;base64,${b64}`;
    }

    console.error(
        "UNEXPECTED IMAGE API RESPONSE:",
        JSON.stringify(response.data),
    );
    throw new Error("Invalid image response structure from xAI");
  } catch (error) {
    if (axios.isAxiosError(error)) {
      const details = error.response ? error.response.data : error.message;
      console.error("IMAGE GENERATION API ERROR:", JSON.stringify(details));

      let msg = "Image generation failed";
      if (error.response && error.response.data && error.response.data.error) {
        msg = error.response.data.error.message;
      } else if (error.message) {
        msg = error.message;
      }
      throw new Error(msg);
    }
    console.error("IMAGE GENERATION GENERIC ERROR:", error);
    throw error;
  }
};
