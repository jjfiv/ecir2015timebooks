package edu.umass.ciir.proteus.athena;

import org.junit.Test;

import static edu.umass.ciir.proteus.athena.dates.DateRecognition.getYear;
import static edu.umass.ciir.proteus.athena.dates.DateRecognition.tryExtractMetadataYear;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

public class DateRecognitionTest {

  @Test
  public void testMetadataAnnoyances() {
    assertNull(tryExtractMetadataYear("[c18?]"));
    assertEquals(1883, (int) tryExtractMetadataYear("[c1883]"));
    assertEquals(1909, (int) tryExtractMetadataYear("1909-1910"));
    assertEquals(1909, (int) tryExtractMetadataYear("(1909-1910)"));
    assertEquals(1909, (int) tryExtractMetadataYear("[1909]1910"));
    assertEquals(1909, (int) tryExtractMetadataYear("1909 1910"));
    assertEquals(1920, (int) tryExtractMetadataYear("[192-]"));
    assertEquals(1990, (int) tryExtractMetadataYear("<1990- >"));
    assertEquals(1782, (int) tryExtractMetadataYear("1909, 1782"));
    assertEquals(1782, (int) tryExtractMetadataYear("-1782"));
    assertEquals(1782, (int) tryExtractMetadataYear("l782")); //lower-case l
  }

  @Test
  public void testGetYear() {
    assertEquals(-27, getYear("-27").intValue());
    assertEquals(27, getYear("27").intValue());
    assertEquals(1927, getYear("1927-05-04").intValue());
    assertEquals(1927, getYear("1927-05").intValue());
    assertNull(getYear("19XX"));
    assertNull(getYear("196X"));
  }


}
