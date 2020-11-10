package com.google.androidbrowserhelper.playbilling.digitalgoods;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.util.Arrays;
import java.util.Collection;

import static com.google.androidbrowserhelper.playbilling.digitalgoods.ItemDetails.toPrice;
import static org.junit.Assert.assertEquals;

/**
 * Tests for {@link ItemDetails#toPrice}.
 */
@RunWith(Parameterized.class)
public class ToPriceTest {
    @Parameterized.Parameters(name = "toPrice({0}) = {1}")
    public static Collection<Object[]> cases() {
        return Arrays.asList(new Object[][] {
                // {    input,      expected },
                // Zero:
                {           0L,   "0.000000" },
                // Positive:
                {           1L,   "0.000001" },
                {          10L,   "0.000010" },
                {         100L,   "0.000100" },
                {       1_000L,   "0.001000" },
                {      10_000L,   "0.010000" },
                {     100_000L,   "0.100000" },
                {   1_000_000L,   "1.000000" },
                {  10_000_000L,  "10.000000" },
                { 100_000_000L, "100.000000" },
                {       1_234L,   "0.001234" },
                { 123_456_789L, "123.456789" },
                // Negative:
                {           -1L,   "-0.000001" },
                {          -10L,   "-0.000010" },
                {         -100L,   "-0.000100" },
                {       -1_000L,   "-0.001000" },
                {      -10_000L,   "-0.010000" },
                {     -100_000L,   "-0.100000" },
                {   -1_000_000L,   "-1.000000" },
                {  -10_000_000L,  "-10.000000" },
                { -100_000_000L, "-100.000000" },
                {       -1_234L,   "-0.001234" },
                { -123_456_789L, "-123.456789" },
        });
    }
    private final long mInput;
    private final String mExpected;

    public ToPriceTest(long input, String expected) {
        mInput = input;
        mExpected = expected;
    }

    @Test
    public void test() {
        assertEquals(mExpected, toPrice(mInput));
    }
}
