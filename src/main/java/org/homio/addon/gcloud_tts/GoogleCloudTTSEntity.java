package org.homio.addon.gcloud_tts;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.google.cloud.texttospeech.v1.SsmlVoiceGender;
import jakarta.persistence.Entity;
import lombok.Getter;
import lombok.Setter;
import lombok.SneakyThrows;
import lombok.experimental.Accessors;
import org.apache.commons.io.IOUtils;
import org.homio.api.Context;
import org.homio.api.entity.media.HasTextToSpeech;
import org.homio.api.entity.types.MiscEntity;
import org.homio.api.exception.NotFoundException;
import org.homio.api.model.ActionResponseModel;
import org.homio.api.model.OptionModel;
import org.homio.api.ui.UISidebarChildren;
import org.homio.api.ui.field.UIField;
import org.homio.api.ui.field.UIFieldGroup;
import org.homio.api.ui.field.UIFieldSlider;
import org.homio.api.ui.field.UIFieldType;
import org.homio.api.ui.field.action.UIContextMenuUploadAction;
import org.homio.api.ui.field.selection.UIFieldStaticSelection;
import org.homio.api.ui.field.selection.dynamic.DynamicOptionLoader;
import org.homio.api.ui.field.selection.dynamic.UIFieldDynamicSelection;
import org.homio.api.util.Lang;
import org.jetbrains.annotations.NotNull;
import org.json.JSONObject;
import org.springframework.http.MediaType;
import org.springframework.web.multipart.MultipartFile;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Set;

@SuppressWarnings({"unused"})
@Getter
@Setter
@Entity
@Accessors(chain = true)
@UISidebarChildren(icon = "fas fa-comment-dots", color = "#349496")
public class GoogleCloudTTSEntity extends MiscEntity
  implements HasTextToSpeech<GoogleCloudTTSService> {

  @UIField(order = 1, hideInEdit = true, hideOnEmpty = true, fullWidth = true, bg = "#334842", type = UIFieldType.HTML)
  public String getDescription() {
    if (!isHasCredentials()) {
      return Lang.getServerMessage("googlecloudtts.description");
    }
    return null;
  }

  @Override
  protected @NotNull String getDevicePrefix() {
    return "gctts";
  }

  @UIField(order = 30, hideInEdit = true)
  public boolean isHasCredentials() {
    return getJsonData().has("credentials");
  }

  @UIField(order = 50, inlineEdit = true)
  @UIFieldGroup(value = "Settings", borderColor = "#0E7ABC")
  @UIFieldDynamicSelection(value = SelectVoice.class, dependencyFields = "languageCode")
  public String getVoice() {
    return getJsonData("voice");
  }

  public void setVoice(String value) {
    setJsonData("voice", value);
  }

  @UIField(order = 60)
  @UIFieldSlider(min = -20, max = 20, step = 0.5)
  @UIFieldGroup(value = "Common", borderColor = "#0EBC97")
  public double getPitch() {
    return getJsonData("pitch", 0D);
  }

  public void setPitch(double value) {
    setJsonData("pitch", value);
  }

  @UIField(order = 70)
  @UIFieldSlider(min = 0.25, max = 4, step = 0.25)
  @UIFieldGroup(value = "Common", borderColor = "#0EBC97")
  public double getSpeakingRate() {
    return getJsonData("speakingRate", 1D);
  }

  public void setSpeakingRate(double value) {
    setJsonData("speakingRate", value);
  }

  @UIField(order = 70)
  @UIFieldSlider(min = -96, max = 16)
  @UIFieldGroup(value = "Common", borderColor = "#0EBC97")
  public double getVolumeGainDb() {
    return getJsonData("volumeGainDb", 0D);
  }

  public void setVolumeGainDb(double value) {
    setJsonData("volumeGainDb", value);
  }

  @UIField(order = 100)
  @UIFieldGroup(value = "Settings", borderColor = "#0E7ABC")
  @UIFieldStaticSelection(value = {"en-US", "en-GB", "fr-FR", "de-DE", "it-IT", "ja-JP", "pl-PL", "ru-RU"
    , "es-US", "uk-UA", "tr-TR"}, rawInput = true)
  public String getLanguageCode() {
    return getJsonData("lang", "en-US");
  }

  public void setLanguageCode(String value) {
    setJsonData("lang", value);
  }

  @UIField(order = 110)
  @UIFieldGroup(value = "Common", borderColor = "#0EBC97")
  public SsmlVoiceGender getSsmlVoiceGender() {
    return getJsonDataEnum("gender", SsmlVoiceGender.NEUTRAL);
  }

  public void setSsmlVoiceGender(SsmlVoiceGender value) {
    setJsonDataEnum("gender", value);
  }

  @Override
  public String getDefaultName() {
    return "Google Cloud TTS";
  }

  @Override
  protected void assembleMissingMandatoryFields(@NotNull Set<String> fields) {
    if (!getJsonData().has("credentials")) {
      fields.add("credentials");
    }
    super.assembleMissingMandatoryFields(fields);
  }

  @Override
  public long getEntityServiceHashCode() {
    return getJsonDataHashCode("lang", "credentials", "gender", "voice", "volumeGainDb", "speakingRate", "pitch");
  }

  @Override
  public @NotNull Class<GoogleCloudTTSService> getEntityServiceItemClass() {
    return GoogleCloudTTSService.class;
  }

  @Override
  public GoogleCloudTTSService createService(@NotNull Context context) {
    return new GoogleCloudTTSService(context, this);
  }

  @SneakyThrows
  @UIContextMenuUploadAction(value = "UPLOAD_CREDENTIALS", icon = "fas fa-upload",
    supportedFormats = {MediaType.APPLICATION_JSON_VALUE})
  public ActionResponseModel uploadCredentials(Context context, JSONObject params) {
    MultipartFile file = ((MultipartFile[]) params.get("files"))[0];
    String credentials = IOUtils.toString(file.getInputStream(), StandardCharsets.UTF_8);
    // test
    GoogleCloudTTSService.generateText(this, "Hello world", credentials);

    setJsonData("credentials", credentials);
    context.db().save(this);

    return ActionResponseModel.showSuccess("ACTION.SUCCESS");
  }

  @JsonIgnore
  public String getCredentials() {
    if (isHasCredentials()) {
      return getJsonData("credentials");
    }
    throw new NotFoundException("Unable to find saved service credentials");
  }

  public static class SelectVoice implements DynamicOptionLoader {

    @Override
    public List<OptionModel> loadOptions(DynamicOptionLoaderParameters parameters) {
      GoogleCloudTTSEntity entity = (GoogleCloudTTSEntity) parameters.getBaseEntity();
      String lang = parameters.getDependencies().getOrDefault("languageCode", entity.getLanguageCode());
      return OptionModel.listWithEmpty(entity.getService().getVoices(lang));
    }
  }
}
