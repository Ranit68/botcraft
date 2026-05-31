const axios = require("axios");

const BASE_URL = "https://api.elevenlabs.io/v1";

/**
 * Gets the configured ElevenLabs API key.
 * @return {string}
 */
function getApiKey() {
  const key = process.env.ELEVEN_API_KEY;

  if (!key) {
    console.error(
        "ELEVEN_API_KEY is not defined in environment variables or secrets",
    );
    throw new Error("Missing ELEVEN_API_KEY");
  }

  return key;
}

/**
 * Builds request headers for ElevenLabs API calls.
 * @return {{"xi-api-key": string, "Content-Type": string}}
 */
function getHeaders() {
  return {
    "xi-api-key": getApiKey(),
    "Content-Type": "application/json",
  };
}

/**
 * Extracts a readable error payload for logging.
 * @param {*} error
 * @return {*}
 */
function getErrorData(error) {
  if (error.response) {
    return {
      status: error.response.status,
      data: error.response.data,
      headers: error.response.headers,
    };
  }
  return error.message;
}

/**
 * Converts text into speech audio.
 * @param {string} text
 * @param {string=} voiceId
 * @return {Promise<string>}
 */
exports.textToSpeech = async (
    text,
    voiceId = "21m00Tcm4TlvDq8ikWAM",
) => {
  try {
    console.log(
        `Generating TTS for text: "${text.substring(0, 20)}..." ` +
        `using voice: ${voiceId}`,
    );

    const headers = getHeaders();
    headers["accept"] = "audio/mpeg";

    const response = await axios.post(
        `${BASE_URL}/text-to-speech/${voiceId}`,
        {
          text,
          model_id: "eleven_multilingual_v2",
          voice_settings: {
            stability: 0.5,
            similarity_boost: 0.5,
          },
        },
        {
          headers: headers,
          responseType: "arraybuffer",
        },
    );

    const base64Audio = Buffer.from(response.data).toString("base64");
    return base64Audio;
  } catch (error) {
    const errorData = getErrorData(error);
    console.error("ELEVEN TTS ERROR:", JSON.stringify(errorData));

    // If it's an arraybuffer, the error data might be a buffer too.
    let message = "Text-to-speech failed";
    if (error.response && error.response.data instanceof Buffer) {
      try {
        const errorJson = JSON.parse(error.response.data.toString());
        message += ": " + (
          errorJson.detail ?
            errorJson.detail.message || errorJson.detail :
            error.response.status
        );
      } catch (e) {
        message += ": " + error.response.status;
      }
    } else {
      message += ": " + (
        error.response ? error.response.status : error.message
      );
    }

    throw new Error(message);
  }
};

/**
 * Lists available voices from ElevenLabs.
 * @return {Promise<Array>}
 */
exports.getVoices = async () => {
  try {
    console.log("Fetching voices from ElevenLabs...");
    const response = await axios.get(`${BASE_URL}/voices`, {
      headers: getHeaders(),
    });

    if (!response.data || !response.data.voices) {
      console.error(
          "Unexpected response format from ElevenLabs:",
          response.data,
      );
      throw new Error("Invalid response format from ElevenLabs");
    }

    return response.data.voices;
  } catch (error) {
    const errorData = getErrorData(error);
    console.error("VOICE LIST ERROR:", JSON.stringify(errorData));
    throw new Error(
        `Failed to fetch voices: ${
          error.response ? error.response.status : error.message
        }`,
    );
  }
};
