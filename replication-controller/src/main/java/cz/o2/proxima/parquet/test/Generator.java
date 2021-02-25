package cz.o2.proxima.parquet.test;

import com.typesafe.config.ConfigFactory;
import cz.o2.proxima.direct.core.OnlineAttributeWriter;
import cz.o2.proxima.parquet.model.Model;
import cz.o2.proxima.parquet.model.proto.Location.LocationSpec;
import cz.o2.proxima.repository.AttributeDescriptor;
import cz.o2.proxima.repository.EntityDescriptor;
import cz.o2.proxima.storage.StreamElement;
import java.util.Random;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class Generator {
  private static final Random random = new Random();

  public static void main(String... args) throws InterruptedException {
    Model model = Model.of(ConfigFactory.load().resolve());
    EntityDescriptor entity = model.getUser().getDescriptor();
    AttributeDescriptor<LocationSpec> attribute = entity.getAttribute("location.*");
    OnlineAttributeWriter writer =
        model
            .directOperator()
            .getWriter(attribute)
            .orElseThrow(
                () -> new IllegalStateException("Unable to get writer for attribute " + attribute));

    int elements = 100;

    for (int i = 0; i < elements; i++) {
      CountDownLatch latch = new CountDownLatch(1);
      writer.write(
          createElement(entity, attribute),
          (sucs, err) -> {
            latch.countDown();
            if (err != null) {
              log.error("Error during writing element.", err);
            }
          });

      latch.await();
    }
    log.info("Written {} elements.", elements);
  }

  private static StreamElement createElement(
      EntityDescriptor entity, AttributeDescriptor<?> attribute) {
    return StreamElement.upsert(
        entity,
        attribute,
        UUID.randomUUID().toString(),
        UUID.randomUUID().toString(),
        attribute.toAttributePrefix(),
        System.currentTimeMillis(),
        LocationSpec.newBuilder()
            .setLatitude(random.nextDouble())
            .setLongitude(random.nextDouble())
            .build()
            .toByteArray());
  }
}
