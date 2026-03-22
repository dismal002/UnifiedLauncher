package com.android.launcher2;

import java.io.IOException;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

/**
 * Minimal XML helpers needed to run the legacy launcher database bootstrap code
 * outside of AOSP (where {@code com.android.internal.util.XmlUtils} is not
 * available).
 */
public final class XmlUtilsCompat {
    private XmlUtilsCompat() {
    }

    public static void beginDocument(XmlPullParser parser, String firstElementName)
            throws XmlPullParserException, IOException {
        int type;
        while ((type = parser.next()) != XmlPullParser.START_TAG
                && type != XmlPullParser.END_DOCUMENT) {
            // Skip until start tag.
        }
        if (type != XmlPullParser.START_TAG) {
            throw new XmlPullParserException("No start tag found");
        }
        if (!firstElementName.equals(parser.getName())) {
            throw new XmlPullParserException(
                    "Unexpected start tag: " + parser.getName() + ", expected: " + firstElementName);
        }
    }
}

