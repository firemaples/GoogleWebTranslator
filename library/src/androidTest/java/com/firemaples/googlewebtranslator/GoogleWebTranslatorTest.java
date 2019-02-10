package com.firemaples.googlewebtranslator;

import android.support.test.InstrumentationRegistry;
import android.support.test.annotation.UiThreadTest;
import android.support.test.filters.SmallTest;
import android.support.test.rule.ActivityTestRule;
import android.support.test.runner.AndroidJUnit4;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

@RunWith(AndroidJUnit4.class)
@SmallTest
public class GoogleWebTranslatorTest {
    private GoogleWebTranslator translator;

    @Rule
    public ActivityTestRule<ActivityForTest> activityTestRule =
            new ActivityTestRule<>(ActivityForTest.class,
                    true,
                    false);

    @Before
    @UiThreadTest
    public void setUp() throws Exception {
        translator = GoogleWebTranslator.init(InstrumentationRegistry.getContext());
        translator.setCallbackOnMainThread(false);
    }

    @After
    public void tearDown() throws Exception {
    }

    @Test
    public void init() {
        assertNotNull(translator);
    }

    @Test
    public void getInstance() {
        assertNotNull(GoogleWebTranslator.getInstance());
    }

    @Test
    public void getNonParentWebView() {
        assertNotNull(translator.getNonParentWebView());
    }

    @Test
    public void addOnTranslationCallback() {
        translator.clearAllCallbacks();
        translator.addOnTranslationCallback(initCallback);
        assertEquals(1, translator.callbackList.size());
    }

    @Test
    public void removeOnTranslationCallback() {
        translator.clearAllCallbacks();
        translator.addOnTranslationCallback(initCallback);
        translator.removeOnTranslationCallback(initCallback);
        assertEquals(0, translator.callbackList.size());
    }

    private GoogleWebTranslator.OnTranslationCallback initCallback = new InitCallback();

    @Test
    public void setTargetLanguage() {
        translator.addOnTranslationCallback(initCallback);
        translator.setTargetLanguage(Language.Chinese_Traditional);
    }

    @Test
    public void setTargetLanguage1() {
        translator.addOnTranslationCallback(initCallback);
        translator.setTargetLanguage("zh-TW");
    }

    @Test
    public void translate() {
        final CountDownLatch signal = new CountDownLatch(1);
        translator.addOnTranslationCallback(new InitCallback() {
            @Override
            public void onInitialized(GoogleWebTranslator translator) {
            }

            @Override
            public void onTranslationSuccess(GoogleWebTranslator translator, TranslatedResult result) {
                translator.removeOnTranslationCallback(this);
                assertEquals("測試1", result.text);
                signal.countDown();
            }
        });
        translator.setTargetLanguage(Language.Chinese_Traditional);
        translator.translate("test");
        try {
            assertTrue("Failed if timeout", signal.await(1, TimeUnit.MINUTES));
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

//    @Test
//    public void translateLargeText() {
//        translator.addOnTranslationCallback(new InitCallback() {
//            @Override
//            public void onInitialized(GoogleWebTranslator translator) {
//                translator.translate("But late on Friday, Mr Trump tweeted: \"We are having very productive talks with North Korea about reinstating the Summit which, if it does happen, will likely remain in Singapore on the same date, June 12th., and, if necessary, will be extended beyond that date.\"");
//            }
//
//            @Override
//            public void onTranslationSuccess(GoogleWebTranslator translator, TranslatedResult result) {
//                translator.removeOnTranslationCallback(this);
//                assertEquals("測試", result.text);
//            }
//        });
//        translator.setTargetLanguage(Language.Chinese_Traditional);
//    }

    private class InitCallback implements GoogleWebTranslator.OnTranslationCallback {
        @Override
        public void onInitialized(GoogleWebTranslator translator) {
            assertTrue("Initialized", true);
        }

        @Override
        public void onInitializationFailed(GoogleWebTranslator translator, int errorCode, String desc) {
            assertTrue("Initialization failed, errorCode: " + errorCode + ", desc: " + desc, false);
        }

        @Override
        public void onTranslationSuccess(GoogleWebTranslator translator, TranslatedResult result) {
            assertTrue("Translated, result: " + result.text, true);
        }

        @Override
        public void onTranslationFailed(GoogleWebTranslator translator, Throwable throwable) {
            assertTrue("Translation failed, error: " + throwable.getMessage(), false);
        }
    }
}