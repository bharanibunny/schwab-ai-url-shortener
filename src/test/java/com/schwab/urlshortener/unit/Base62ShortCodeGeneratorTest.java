package com.schwab.urlshortener.unit;

import com.schwab.urlshortener.util.Base62ShortCodeGenerator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class Base62ShortCodeGeneratorTest {

    private Base62ShortCodeGenerator generator;

    @BeforeEach
    void setUp() {
        generator = new Base62ShortCodeGenerator();
    }

    @Test
    void generateCode_defaultLength_returns7CharacterBase62String() {
        String code = generator.generateCode();
        assertNotNull(code);
        assertEquals(7, code.length());
        assertTrue(code.matches("^[A-Za-z0-9]{7}$"));
    }

    @Test
    void generateCode_customLength_returnsCorrectLength() {
        String code = generator.generateCode(10);
        assertNotNull(code);
        assertEquals(10, code.length());
        assertTrue(code.matches("^[A-Za-z0-9]{10}$"));
    }

    @Test
    void generateCode_invalidLength_throwsIllegalArgumentException() {
        assertThrows(IllegalArgumentException.class, () -> generator.generateCode(0));
        assertThrows(IllegalArgumentException.class, () -> generator.generateCode(-5));
    }

    @Test
    void generateCode_producesUniqueCodes() {
        Set<String> generatedCodes = new HashSet<>();
        int count = 1000;
        for (int i = 0; i < count; i++) {
            generatedCodes.add(generator.generateCode());
        }
        assertEquals(count, generatedCodes.size(), "Generated codes should be highly unique and collision-free for 1000 iterations");
    }
}
