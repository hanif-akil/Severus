package rkr.simplekeyboard.inputmethod.latin.utils

import android.content.res.TypedArray
import org.xmlpull.v1.XmlPullParser
import org.xmlpull.v1.XmlPullParserException
import java.io.IOException

object XmlParseUtils {
    open class ParseException : XmlPullParserException {
        constructor(msg: String) : super(msg)
        constructor(msg: String, parser: XmlPullParser) : super("$msg at ${parser.positionDescription}")
    }

    class IllegalStartTag(parser: XmlPullParser, tag: String, parent: String) :
        ParseException("Illegal start tag $tag in $parent", parser)

    class IllegalEndTag(parser: XmlPullParser, tag: String, parent: String) :
        ParseException("Illegal end tag $tag in $parent", parser)

    class IllegalAttribute(parser: XmlPullParser, tag: String, attribute: String) :
        ParseException("Tag $tag has illegal attribute $attribute", parser)

    class NonEmptyTag(parser: XmlPullParser, tag: String) :
        ParseException("$tag must be empty tag", parser)

    @Throws(XmlPullParserException::class, IOException::class)
    fun checkEndTag(tag: String, parser: XmlPullParser) {
        if (parser.next() == XmlPullParser.END_TAG && tag == parser.name) return
        throw NonEmptyTag(parser, tag)
    }

    @Throws(XmlPullParserException::class)
    fun checkAttributeExists(attr: TypedArray, attrId: Int, attrName: String, tag: String, parser: XmlPullParser) {
        if (attr.hasValue(attrId)) return
        throw ParseException("No $attrName attribute found in <$tag/>", parser)
    }
}
