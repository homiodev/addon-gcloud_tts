package org.touchhome.bundle.tts;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.google.cloud.texttospeech.v1.SsmlVoiceGender;
import java.nio.charset.StandardCharsets;
import java.util.List;
import javax.persistence.Entity;
import lombok.Getter;
import lombok.Setter;
import lombok.SneakyThrows;
import lombok.experimental.Accessors;
import org.apache.commons.io.IOUtils;
import org.json.JSONObject;
import org.springframework.http.MediaType;
import org.springframework.web.multipart.MultipartFile;
import org.touchhome.bundle.api.EntityContext;
import org.touchhome.bundle.api.entity.HasStatusAndMsg;
import org.touchhome.bundle.api.entity.types.MiscEntity;
import org.touchhome.bundle.api.exception.NotFoundException;
import org.touchhome.bundle.api.model.ActionResponseModel;
import org.touchhome.bundle.api.model.OptionModel;
import org.touchhome.bundle.api.service.EntityService;
import org.touchhome.bundle.api.ui.UISidebarChildren;
import org.touchhome.bundle.api.ui.action.DynamicOptionLoader;
import org.touchhome.bundle.api.ui.field.UIField;
import org.touchhome.bundle.api.ui.field.UIFieldGroup;
import org.touchhome.bundle.api.ui.field.UIFieldProgress;
import org.touchhome.bundle.api.ui.field.UIFieldSlider;
import org.touchhome.bundle.api.ui.field.UIFieldType;
import org.touchhome.bundle.api.ui.field.action.UIContextMenuUploadAction;
import org.touchhome.bundle.api.ui.field.selection.UIFieldSelection;
import org.touchhome.bundle.api.ui.field.selection.UIFieldStaticSelection;
import org.touchhome.bundle.api.util.Lang;

@Getter
@Setter
@Entity
@Accessors(chain = true)
@UISidebarChildren(icon = "fas fa-comment-dots", color = "#349496")
public class GoogleCloudTTSEntity extends MiscEntity<GoogleCloudTTSEntity>
    implements EntityService<GoogleCloudTTSService, GoogleCloudTTSEntity>,
    HasStatusAndMsg<GoogleCloudTTSEntity> {

  public static final String PREFIX = "gctts_";

  @UIField(order = 1, hideInEdit = true, hideOnEmpty = true, fullWidth = true, bg = "#334842", type = UIFieldType.HTML)
  public String getDescription() {
    if (!isHasCredentials()) {
      return Lang.getServerMessage("googlecloudtts.description");
    }
    return null;
  }

  @UIField(order = 30, hideInEdit = true)
  public boolean isHasCredentials() {
    return getJsonData().has("credentials");
  }

  @UIField(order = 40, hideInEdit = true)
  @UIFieldProgress(fillColor = "#8f8359", color = "#504c3e")
  public UIFieldProgress.Progress getUsedQuota() {
    int current = new GoogleCloudTTSService(this, null).getSynthesizedCharacters();
    return UIFieldProgress.Progress.of(current, getMaxCharactersQuota());
  }

  @UIField(order = 40, hideInView = true)
  public int getMaxCharactersQuota() {
    return getJsonData("quota", 1000000);
  }

  public void setMaxCharactersQuota(int value) {
    setJsonData("quota", value);
  }

  @UIField(order = 45)
  public boolean isDisableTranslateAfterQuota() {
    return getJsonData("disOverQuota", true);
  }

  public void setDisableTranslateAfterQuota(boolean value) {
    setJsonData("disOverQuota", value);
  }

  @UIField(order = 50, inlineEdit = true)
  @UIFieldGroup(value = "Settings", borderColor = "#0E7ABC")
  @UIFieldSelection(value = SelectVoice.class, dependencyFields = "languageCode")
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
      , "es-US", "uk-UA", "tr-TR"}, allowInputRawText = true)
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
  public String getEntityPrefix() {
    return PREFIX;
  }

  @Override
  public Class<GoogleCloudTTSService> getEntityServiceItemClass() {
    return GoogleCloudTTSService.class;
  }

  @Override
  public GoogleCloudTTSService createService(EntityContext entityContext) {
    return new GoogleCloudTTSService(this);
  }

  @SneakyThrows
  @UIContextMenuUploadAction(value = "UPLOAD_CREDENTIALS", icon = "fas fa-upload",
      supportedFormats = {MediaType.APPLICATION_JSON_VALUE})
  public ActionResponseModel uploadCredentials(EntityContext entityContext, JSONObject params) {
    MultipartFile file = ((MultipartFile[]) params.get("files"))[0];
    String credentials = IOUtils.toString(file.getInputStream(), StandardCharsets.UTF_8);
    new GoogleCloudTTSService(GoogleCloudTTSEntity.this, credentials).testServiceWithSetStatus();

    setJsonData("credentials", credentials);
    entityContext.save(this);

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
