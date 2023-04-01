package org.homio.bundle.tts;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.homio.bundle.api.BundleEntrypoint;
import org.springframework.stereotype.Component;

@Log4j2
@Component
@RequiredArgsConstructor
public class GoogleCloudTTSEntrypoint implements BundleEntrypoint {

  public void init() {

  }

  @Override
  public int order() {
    return 800;
  }
}
