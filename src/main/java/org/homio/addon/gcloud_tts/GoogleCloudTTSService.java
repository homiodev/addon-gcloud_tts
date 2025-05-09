package org.homio.addon.gcloud_tts;

import com.google.api.gax.core.CredentialsProvider;
import com.google.api.gax.core.FixedCredentialsProvider;
import com.google.auth.oauth2.ServiceAccountCredentials;
import com.google.cloud.texttospeech.v1.AudioConfig;
import com.google.cloud.texttospeech.v1.AudioEncoding;
import com.google.cloud.texttospeech.v1.SynthesisInput;
import com.google.cloud.texttospeech.v1.SynthesizeSpeechResponse;
import com.google.cloud.texttospeech.v1.TextToSpeechClient;
import com.google.cloud.texttospeech.v1.TextToSpeechSettings;
import com.google.cloud.texttospeech.v1.Voice;
import com.google.cloud.texttospeech.v1.VoiceSelectionParams;
import com.google.protobuf.ByteString;
import lombok.SneakyThrows;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.lang3.StringUtils;
import org.homio.api.Context;
import org.homio.api.service.TextToSpeechEntityService;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@Log4j2
public class GoogleCloudTTSService extends TextToSpeechEntityService<GoogleCloudTTSEntity> {

  public GoogleCloudTTSService(Context context, GoogleCloudTTSEntity entity) {
    super(context, entity, "GoogleCloudTTS", entity.isDisableTranslateAfterQuota() ? null :
      entity.getMaxCharactersQuota(), "Google Cloud TTS");
  }

  protected static byte[] generateText(GoogleCloudTTSEntity entity, String text, String credentials) throws IOException {
    log.info("Start synthesize <{}>", text);
    ByteArrayInputStream stream = new ByteArrayInputStream(credentials.getBytes());
    CredentialsProvider credentialsProvider = FixedCredentialsProvider.create(ServiceAccountCredentials.fromStream(stream));
    TextToSpeechSettings settings = TextToSpeechSettings.newBuilder().setCredentialsProvider(credentialsProvider).build();
    try (TextToSpeechClient textToSpeechClient = TextToSpeechClient.create(settings)) {
      SynthesisInput input = SynthesisInput.newBuilder().setText(text).build();
      VoiceSelectionParams.Builder voice = VoiceSelectionParams.newBuilder()
        .setLanguageCode(entity.getLanguageCode())
        .setSsmlGender(entity.getSsmlVoiceGender());
      if (StringUtils.isNotEmpty(entity.getVoice())) {
        voice.setName(entity.getVoice());
      }

      // Select the type of audio file you want returned
      AudioConfig audioConfig = AudioConfig.newBuilder()
        .setVolumeGainDb(entity.getVolumeGainDb())
        .setAudioEncoding(AudioEncoding.MP3)
        .setPitch(entity.getPitch())
        .setSpeakingRate(entity.getSpeakingRate())
        .build();

      // Perform the text-to-speech request on the text input with the selected voice parameters and
      // audio file type
      SynthesizeSpeechResponse response = textToSpeechClient.synthesizeSpeech(input, voice.build(), audioConfig);

      // Get the audio contents from the response
      ByteString audioContents = response.getAudioContent();

      // Write the response to the output file.
      log.info("Finish synthesize");

      return audioContents.toByteArray();
    }
  }

  @Override
  @SneakyThrows
  protected byte[] synthesizeSpeech(String text) {
    return generateText(entity, text, entity.getCredentials());
  }

  @Override
  public String getUniqueFilenameForText() {
    return String.join("_", Arrays.asList(
      entity.getLanguageCode(),
      entity.getVoice(),
      String.valueOf(entity.getVolumeGainDb()),
      String.valueOf(entity.getSpeakingRate()),
      String.valueOf(entity.getPitch()),
      entity.getSsmlVoiceGender().name()));
  }

  @Override
  @SneakyThrows
  public List<String> getVoices(String languageCode) {
    if (!cacheVoices.containsKey(languageCode)) {
      ByteArrayInputStream stream = new ByteArrayInputStream(entity.getCredentials().getBytes());
      CredentialsProvider credentialsProvider =
        FixedCredentialsProvider.create(ServiceAccountCredentials.fromStream(stream));
      TextToSpeechSettings settings = TextToSpeechSettings.newBuilder().setCredentialsProvider(credentialsProvider).build();
      try (TextToSpeechClient textToSpeechClient = TextToSpeechClient.create(settings)) {
        List<String> voices = textToSpeechClient.listVoices(languageCode)
          .getVoicesList().stream().map(Voice::getName).collect(Collectors.toList());
        cacheVoices.put(languageCode, voices);
      }
    }
    return cacheVoices.get(languageCode);
  }

  @Override
  protected void initialize() {

  }
}
