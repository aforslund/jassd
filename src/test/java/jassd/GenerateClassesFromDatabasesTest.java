/*
 * Copyright 2021 Andreas Forslund.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package jassd;

import java.io.PrintWriter;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assertions.assertEquals;


/**
 * Executes a number of basic tests, primarily for formatting
 * 
 * @author andreas.s.forslund@gmail.com
 */
public class GenerateClassesFromDatabasesTest {
    
    private GenerateClassesFromDatabase gcd;
    
    public GenerateClassesFromDatabasesTest() {
        gcd = new GenerateClassesFromDatabase();
    }
    
    @BeforeAll
    public static void setUpClass() {
    }
    
    @AfterAll
    public static void tearDownClass() {
    }
    
    @BeforeEach
    public void setUp() {
        
    }
    
    @AfterEach
    public void tearDown() {
    }

    @Test
    public void testConvertSqlNameToProperty() {
        assertEquals("userId",gcd.convertSqlNameToProperty("user_id"));
        assertEquals("oneTwoThree",gcd.convertSqlNameToProperty("one_two_three"));
        assertEquals("userid",gcd.convertSqlNameToProperty("userid"));
    }
    @Test
    public void testSpacing() {
        assertEquals("hello", gcd.setSpacing("hello", 0));
        assertEquals("  hello", gcd.setSpacing("hello", 2));
        assertEquals("     hello", gcd.setSpacing("hello", 5));
    }
    @Test
    public void testAutoIndent() {
        gcd.pw = new PrintWriter(System.out);
        gcd.curSpacing = 0;
        gcd.spacing = 2;
        assertEquals("hello {", gcd.println("hello {"));
        assertEquals("  hello2", gcd.println("hello2"));
        assertEquals("}", gcd.println("}"));
    }
    @Test
    public void testCapitalization() {
        assertEquals("Int", gcd.capitalize("int"));
        assertEquals("String", gcd.capitalize("String"));
    }
}
