package com.donut.launcher;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

import java.io.IOException;

final class XmlUtils {
    private XmlUtils() {
    }

    static void beginDocument(XmlPullParser parser, String firstElementName)
            throws XmlPullParserException, IOException {
        int type;
        while ((type = parser.next()) != XmlPullParser.START_TAG
                && type != XmlPullParser.END_DOCUMENT) {
            // Skip until first start tag.
        }

        if (type != XmlPullParser.START_TAG) {
            throw new XmlPullParserException("No start tag found");
        }

        if (!firstElementName.equals(parser.getName())) {
            throw new XmlPullParserException(
                    "Unexpected start tag: found " + parser.getName()
                            + ", expected " + firstElementName);
        }
    }
}
