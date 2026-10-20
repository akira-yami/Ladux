package org.akira.ladux.assistant.util;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class SmartFitSpecsTest {
    @Test
    void parsesExplicitUnitsAndConvertsTerabytesWithoutReadingBusFrequency() {
        assertEquals(16, SmartFitSpecs.ramGb("16GB DDR5 5600MHz"));
        assertEquals(32, SmartFitSpecs.ramGb("LPDDR5 32 GiB"));
        assertEquals(16, SmartFitSpecs.ramGb("LPDDR5X 16GB"));
        assertEquals(16, SmartFitSpecs.ramGb("16GB LPDDR5X 7500MHz"));
        assertEquals(1024, SmartFitSpecs.ssdGb("1TB SSD NVMe PCIe 4.0"));
        assertEquals(512, SmartFitSpecs.ssdGb("NVMe 0,5 TB"));
        assertNull(SmartFitSpecs.ramGb("DDR5 5600MHz"));
        assertNull(SmartFitSpecs.ssdGb("512GB"), "ROM capacity alone does not confirm SSD technology");
        assertNull(SmartFitSpecs.ssdGb("1TB HDD"));
    }

    @Test
    void ambiguousMalformedOrOutOfRangeSpecsStayUnknown() {
        for (String value : new String[] {"", "unknown", "-8GB", "8-16GB", "8GB + 8GB", "2 x 8GB", "8GB x2", "2×8GB", "16GB / 32GB", "999999999999TB", "0GB", "1.3GB", "up to 64 GB", "hỗ trợ 32GB", "-8GB\n"}) {
            assertNull(SmartFitSpecs.ramGb(value), value);
        }
        assertNull(SmartFitSpecs.ssdGb("512GB SSD + 1TB HDD"));
        assertNull(SmartFitSpecs.ssdGb("512GB SSD / 1TB SSD"));
        assertNull(SmartFitSpecs.ramGb(null));
    }

    @Test
    void weightRequiresAnUnambiguousPlausibleMeasurement() {
        assertEquals(1.24, SmartFitSpecs.weightKg("1.24 kg"));
        assertEquals(1.5, SmartFitSpecs.weightKg("1500 g"));
        assertEquals(1.36, SmartFitSpecs.weightKg("1,36 kg"));
        assertNull(SmartFitSpecs.weightKg("1.5"));
        assertNull(SmartFitSpecs.weightKg("1.3–1.6 kg"));
        assertNull(SmartFitSpecs.weightKg("0 kg"));
        assertNull(SmartFitSpecs.weightKg("200 kg"));
    }

    @Test
    void hardwareRulesRecognizeFamiliesWithoutInventingRelativeBenchmarks() {
        assertTrue(SmartFitSpecs.recognizedCpu("Intel Core i7-13650HX"));
        assertTrue(SmartFitSpecs.recognizedCpu("AMD Ryzen 7 8845HS"));
        assertTrue(SmartFitSpecs.recognizedCpu("Apple M3 Pro 11-Core CPU"));
        assertFalse(SmartFitSpecs.recognizedCpu("Mystery 9900 ultra fast"));
        assertTrue(SmartFitSpecs.discreteGpu("NVIDIA GeForce RTX 4050 6GB GDDR6"));
        assertTrue(SmartFitSpecs.discreteGpu("AMD Radeon RX 7600M"));
        assertFalse(SmartFitSpecs.discreteGpu("AMD Radeon 780M Graphics"));
        assertFalse(SmartFitSpecs.discreteGpu("Intel Arc Graphics"));
        assertTrue(SmartFitSpecs.appleGpu("Apple M3 Pro 14-Core GPU"));
    }
}
