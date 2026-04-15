const axios = require("axios");

const BASE_URL = "https://api.elevenlabs.io/v1";

/**
 * Gets the configured ElevenLabs API key.
 * @return {string}
 */
function getApiKey() {
  const key = process.env.ELEVEN_API_KEY;

  if (!key) {
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
  return error.response ? error.response.data : error.message;
}

/**
 * Converts text into speech audio.
 * @param {string} text
 * @param {string=} voiceId
 * @return {Promise<string>}
 */
exports.textToSpeech = async (text, voiceId = "21m00Tcm4TlvDq8ikWAM") => {
  try {
    const response = await axios.post(
        `${BASE_URL}/text-to-speech/${voiceId}`,
        {
          text,
          model_id: "eleven_multilingual_v2",
        },
        {
          headers: getHeaders(),
          responseType: "arraybuffer",
        },
    );

    const base64Audio = Buffer.from(response.data).toString("base64");
    return `data:audio/mpeg;base64,${base64Audio}`;
  } catch (error) {
    console.error("ELEVEN TTS ERROR:", getErrorData(error));
    throw new Error("Text-to-speech failed");
  }
};

/**
 * Transcribes speech from a remote audio URL.
 * @param {string} audioUrl
 * @return {Promise<string>}
 */
exports.speechToText = async (audioUrl) => {
  try {
    const response = await axios.post(
        `${BASE_URL}/speech-to-text`,
        {
          audio_url: audioUrl,
        },
        {
          headers: getHeaders(),
        },
    );

    return response.data.text;
  } catch (error) {
    console.error("ELEVEN STT ERROR:", getErrorData(error));
    throw new Error("Speech-to-text failed");
  }
};

/**
 * Lists available voices from ElevenLabs.
 * @return {Promise<Array>}
 */
exports.getVoices = async () => {
  try {
    const response = await axios.get(`${BASE_URL}/voices`, {
      headers: getHeaders(),
    });

    return response.data.voices;
  } catch (error) {
    console.error("VOICE LIST ERROR:", getErrorData(error));
    throw new Error("Failed to fetch voices");
  }
};

/**
 * Converts one speech sample into another voice.
 * @param {string} audioBase64
 * @param {string} voiceId
 * @return {Promise<string>}
 */
exports.speechToSpeech = async (audioBase64, voiceId) => {
  try {
    const response = await axios.post(
        `${BASE_URL}/speech-to-speech/${voiceId}`,
        {
          audio: audioBase64,
        },
        {
          headers: getHeaders(),
          responseType: "arraybuffer",
        },
    );

    return Buffer.from(response.data).toString("base64");
  } catch (error) {
    console.error("VOICE CLONE ERROR:", getErrorData(error));
    throw new Error("Speech-to-speech failed");
  }
};
