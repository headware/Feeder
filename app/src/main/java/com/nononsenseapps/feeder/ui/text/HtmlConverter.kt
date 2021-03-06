package com.nononsenseapps.feeder.ui.text


import android.content.Context
import android.graphics.Point
import android.text.Spanned
import org.ccil.cowan.tagsoup.HTMLSchema
import org.ccil.cowan.tagsoup.Parser
import java.net.URL


val schema: HTMLSchema by lazy { HTMLSchema() }

fun toSpannedWithImages(context: Context,
                        source: String,
                        siteUrl: URL,
                        maxSize: Point,
                        allowDownload: Boolean,
                        spannableStringBuilder: SensibleSpannableStringBuilder = SensibleSpannableStringBuilder()): Spanned {
    val parser = Parser()
    try {
        parser.setProperty(Parser.schemaProperty, schema)
    } catch (e: org.xml.sax.SAXNotRecognizedException) {
        // Should not happen.
        throw RuntimeException(e)
    } catch (e: org.xml.sax.SAXNotSupportedException) {
        throw RuntimeException(e)
    }

    val converter = GlideConverter(context, source, siteUrl, parser, maxSize, allowDownload, spannableStringBuilder)
    return converter.convert()
}

/**
 * Returns displayable styled text from the provided HTML string.
 * Any &lt;img&gt; tags in the HTML will use the specified ImageGetter
 * to request a representation of the image (use null if you don't
 * want this) and the specified TagHandler to handle unknown tags
 * (specify null if you don't want this).
 *
 *
 *
 * This uses TagSoup to handle real HTML, including all of the brokenness
 * found in the wild.
 */
fun toSpannedWithNoImages(context: Context, source: String, siteUrl: URL,
                          spannableStringBuilder: SensibleSpannableStringBuilder = SensibleSpannableStringBuilder()): Spanned {
    val parser = Parser()
    try {
        parser.setProperty(Parser.schemaProperty, schema)
    } catch (e: org.xml.sax.SAXNotRecognizedException) {
        // Should not happen.
        throw RuntimeException(e)
    } catch (e: org.xml.sax.SAXNotSupportedException) {
        throw RuntimeException(e)
    }

    val converter = HtmlToSpannedConverter(source, siteUrl, parser, context, spannableStringBuilder)
    return converter.convert()
}
