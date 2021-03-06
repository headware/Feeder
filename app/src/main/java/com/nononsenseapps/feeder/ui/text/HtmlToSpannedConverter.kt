package com.nononsenseapps.feeder.ui.text

import android.content.Context
import android.content.Intent
import android.content.res.Resources
import android.graphics.Color
import android.graphics.Typeface
import android.text.Layout
import android.text.Spannable
import android.text.Spanned
import android.text.TextUtils
import android.text.style.AlignmentSpan
import android.text.style.BackgroundColorSpan
import android.text.style.BulletSpan
import android.text.style.ImageSpan
import android.text.style.LeadingMarginSpan
import android.text.style.ParagraphStyle
import android.text.style.RelativeSizeSpan
import android.text.style.StyleSpan
import android.text.style.SubscriptSpan
import android.text.style.SuperscriptSpan
import android.text.style.TextAppearanceSpan
import android.text.style.TypefaceSpan
import android.text.style.UnderlineSpan
import com.nononsenseapps.feeder.R
import com.nononsenseapps.feeder.ui.ARG_URL
import com.nononsenseapps.feeder.ui.ReaderWebViewActivity
import com.nononsenseapps.feeder.ui.SHOULD_FINISH_BACK
import com.nononsenseapps.feeder.util.PREF_VAL_OPEN_WITH_WEBVIEW
import com.nononsenseapps.feeder.util.PrefUtils.shouldOpenLinkWith
import com.nononsenseapps.feeder.util.openLinkInBrowser
import com.nononsenseapps.feeder.util.relativeLinkIntoAbsolute
import org.ccil.cowan.tagsoup.Parser
import org.xml.sax.Attributes
import org.xml.sax.ContentHandler
import org.xml.sax.InputSource
import org.xml.sax.Locator
import org.xml.sax.SAXException
import org.xml.sax.XMLReader
import java.io.IOException
import java.io.StringReader
import java.net.URL

/**
 * Convert an HTML document into a spannable string.
 */
@Suppress("UNUSED_PARAMETER")
open class HtmlToSpannedConverter(private var mSource: String,
                                  private var mSiteUrl: URL,
                                  parser: Parser,
                                  private val mContext: Context,
                                  private val spannableStringBuilder: SensibleSpannableStringBuilder = SensibleSpannableStringBuilder()) : ContentHandler {
    private var mAccentColor: Int = 0
    private var mQuoteGapWidth: Int = 0
    private var mQuoteStripeWidth: Int = 0
    private var ignoreCount = 0
    private var respectFormatting: Int = 0
    private var mReader: XMLReader = parser
    private var ignoredImage = false

    private val ignoredTags = listOf("style", "script")

    private val urlClickListener: ((String, Context) -> Unit) = { link, context ->
        when (shouldOpenLinkWith(context)) {
            PREF_VAL_OPEN_WITH_WEBVIEW -> {
                val intent = Intent(context, ReaderWebViewActivity::class.java)
                intent.putExtra(SHOULD_FINISH_BACK, true)
                intent.putExtra(ARG_URL, link)
                context.startActivity(intent)
            }
            else -> {
                openLinkInBrowser(context, link)
            }
        }
    }

    init {
        @Suppress("DEPRECATION")
        mAccentColor = mContext.resources.getColor(R.color.accent)
        mQuoteGapWidth = Math.round(mContext.resources.getDimension(R.dimen.reader_quote_gap_width))
        mQuoteStripeWidth = Math.round(mContext.resources.getDimension(R.dimen.reader_quote_stripe_width))
    }

    fun convert(): Spanned {

        mReader.contentHandler = this
        try {
            mReader.parse(InputSource(StringReader(mSource)))
        } catch (e: IOException) {
            // We are reading from a string. There should not be IO problems.
            throw RuntimeException(e)
        } catch (e: SAXException) {
            // TagSoup doesn't throw parse exceptions.
            throw RuntimeException(e)
        }

        // Fix flags and range for paragraph-type markup.
        val obj = spannableStringBuilder.getAllSpansWithType<ParagraphStyle>()
        for (anObj in obj) {
            val start = spannableStringBuilder.getSpanStart(anObj)
            var end = spannableStringBuilder.getSpanEnd(anObj)

            // If the last line of the range is blank, back off by one.
            if (end - 2 >= 0) {
                if (spannableStringBuilder[end - 1] == '\n' && spannableStringBuilder[end - 2] == '\n') {
                    end--
                }
            }

            if (end == start) {
                spannableStringBuilder.removeSpan(anObj)
            }
            //            else {
            //                spannableStringBuilder
            //                        .setSpan(obj[i], start, end, Spannable.SPAN_PARAGRAPH);
            //            }
        }

        return spannableStringBuilder
    }

    override fun setDocumentLocator(locator: Locator) {}

    @Throws(SAXException::class)
    override fun startDocument() {
    }

    @Throws(SAXException::class)
    override fun endDocument() {
    }

    @Throws(SAXException::class)
    override fun startPrefixMapping(prefix: String, uri: String) {
    }

    @Throws(SAXException::class)
    override fun endPrefixMapping(prefix: String) {
    }

    @Throws(SAXException::class)
    override fun startElement(uri: String, localName: String, qName: String,
                              attributes: Attributes) {
        handleStartTag(localName, attributes)
    }

    private fun handleStartTag(tag: String, attributes: Attributes) {

        when {
            tag.equals("br", ignoreCase = true) -> {
                // We don't need to handle this. TagSoup will ensure that there's a </br> for each <br>
                // so we can safely emit the linebreaks when we handle the close tag.
            }
            tag.equals("p", ignoreCase = true) -> handleP(spannableStringBuilder)
            tag.equals("div", ignoreCase = true) -> handleP(spannableStringBuilder)
            tag.equals("strong", ignoreCase = true) -> start(spannableStringBuilder, Bold())
            tag.equals("b", ignoreCase = true) -> start(spannableStringBuilder, Bold())
            tag.equals("em", ignoreCase = true) -> start(spannableStringBuilder, Italic())
            tag.equals("cite", ignoreCase = true) -> start(spannableStringBuilder, Italic())
            tag.equals("dfn", ignoreCase = true) -> start(spannableStringBuilder, Italic())
            tag.equals("i", ignoreCase = true) -> start(spannableStringBuilder, Italic())
            tag.equals("big", ignoreCase = true) -> start(spannableStringBuilder, Big())
            tag.equals("small", ignoreCase = true) -> start(spannableStringBuilder, Small())
            tag.equals("font", ignoreCase = true) -> startFont(spannableStringBuilder, attributes)
            tag.equals("blockquote", ignoreCase = true) -> {
                handleP(spannableStringBuilder)
                start(spannableStringBuilder, Blockquote())
            }
            tag.equals("tt", ignoreCase = true) -> start(spannableStringBuilder, Monospace())
            tag.equals("a", ignoreCase = true) -> startA(spannableStringBuilder, attributes)
            tag.equals("u", ignoreCase = true) -> start(spannableStringBuilder, Underline())
            tag.equals("sup", ignoreCase = true) -> start(spannableStringBuilder, Super())
            tag.equals("sub", ignoreCase = true) -> start(spannableStringBuilder, Sub())
            tag.length == 2 &&
                    Character.toLowerCase(tag[0]) == 'h' &&
                    tag[1] >= '1' && tag[1] <= '6' -> {
                handleP(spannableStringBuilder)
                start(spannableStringBuilder, Header(tag[1] - '1'))
            }
            tag.equals("img", ignoreCase = true) -> startImg(spannableStringBuilder, attributes)
            tag.equals("ul", ignoreCase = true) -> startUl(spannableStringBuilder, attributes)
            tag.equals("ol", ignoreCase = true) -> startOl(spannableStringBuilder, attributes)
            tag.equals("li", ignoreCase = true) -> startLi(spannableStringBuilder, attributes)
            tag.equals("pre", ignoreCase = true) -> startPre(spannableStringBuilder, attributes)
            tag.equals("code", ignoreCase = true) -> startCode(spannableStringBuilder, attributes)
            tag.equals("iframe", ignoreCase = true) -> startIframe(spannableStringBuilder, attributes)
            tag.equals("td", ignoreCase = true) || tag.equals("th", ignoreCase = true) -> {
                startTableCol(spannableStringBuilder)
            }
            tag.equals("tr", ignoreCase = true) -> startEndTableRow(spannableStringBuilder)
            tag.equals("table", ignoreCase = true) -> startEndTable(spannableStringBuilder)
            tag.toLowerCase() in ignoredTags -> ignoreCount++
            else -> startUnknownTag(tag, spannableStringBuilder, attributes)
        }
    }

    private fun handleP(text: SensibleSpannableStringBuilder) {
        ensureDoubleNewline(text)
    }

    protected fun start(text: SensibleSpannableStringBuilder, mark: Any) {
        val len = text.length
        text.setSpan(mark, len, len, Spannable.SPAN_MARK_MARK)
    }

    private fun startFont(text: SensibleSpannableStringBuilder,
                          attributes: Attributes) {
        val color: String? = attributes.getValue("", "color")
        val face: String? = attributes.getValue("", "face")

        val len = text.length
        // Use empty string to prevent null pointer errors. empty string will be ignored in endFont.
        text.setSpan(Font(color ?: "", face), len, len, Spannable.SPAN_MARK_MARK)
    }

    private fun startA(text: SensibleSpannableStringBuilder,
                       attributes: Attributes) {
        var href: String? = attributes.getValue("", "href")

        if (href != null) {
            // Yes, this was an observed null pointer exception
            href = relativeLinkIntoAbsolute(mSiteUrl, href)
        }

        val len = text.length
        text.setSpan(Href(href), len, len, Spannable.SPAN_MARK_MARK)
    }

    protected open fun startImg(text: SensibleSpannableStringBuilder,
                                attributes: Attributes) {
        // Override me
        val width: String? = attributes.getValue("", "width")
        val height: String? = attributes.getValue("", "height")

        var shouldIgnore = false

        if (width != null) {
            try {
                if (width.toInt() < 2) {
                    shouldIgnore = true
                }
            } catch (_: NumberFormatException) {
                shouldIgnore = true
            }
        }
        if (height != null) {
            try {
                if (height.toInt() < 2) {
                    shouldIgnore = true
                }
            } catch (_: NumberFormatException) {
                shouldIgnore = true
            }
        }

        if (shouldIgnore) {
            ignoredImage = true
            return
        }

        var src: String? = attributes.getValue("", "src")
        @Suppress("DEPRECATION")
        val d = mContext.resources.getDrawable(R.drawable.placeholder_image_article)
        d.setBounds(0, 0, d.intrinsicWidth, d.intrinsicHeight)


        val len = text.length
        text.append("\uFFFC")

        if (src == null) {
            src = ""
        }
        val imgLink = relativeLinkIntoAbsolute(mSiteUrl, src)

        text.setSpan(ImageSpan(d, imgLink), len, text.length,
                Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
        // Add a line break
        text.append("\n")
    }

    private fun startUl(text: SensibleSpannableStringBuilder,
                        attributes: Attributes) {
        // Start lists with linebreak
        val len = text.length
        if (len < 1 || text[len - 1] != '\n') {
            text.append("\n")
        }

        // Remember list type
        start(text, Listing(false))
    }

    private fun startOl(text: SensibleSpannableStringBuilder,
                        attributes: Attributes) {
        // Start lists with linebreak
        val len = text.length
        if (len < 1 || text[len - 1] != '\n') {
            text.append("\n")
        }

        // Remember list type
        start(text, Listing(true))
    }

    private fun startLi(text: SensibleSpannableStringBuilder,
                        attributes: Attributes) {
        // Get type of list
        val list: Listing? = getLast(text, Listing::class.java)

        if (list!!.ordered) {
            // Numbered
            // Add number in bold
            start(text, Bold())
            text.append("" + list.number++).append(". ")
            end(text, Bold::class.java, StyleSpan(Typeface.BOLD))
            // Then do a leading margin
            start(text, CountBullet())
        } else {
            // Bullet
            start(text, Bullet())
        }
    }

    private fun startPre(text: SensibleSpannableStringBuilder,
                         attributes: Attributes) {
        respectFormatting++
        ensureDoubleNewline(text)
        start(text, Pre())
    }

    private fun startCode(text: SensibleSpannableStringBuilder,
                          attributes: Attributes) {
        start(text, Code())
    }

    protected open fun startIframe(text: SensibleSpannableStringBuilder,
                                   attributes: Attributes) {
        // Override me
    }

    private fun startUnknownTag(tag: String, text: SensibleSpannableStringBuilder,
                                attr: Attributes) {
        // Override me
    }

    @Suppress("UNUSED_PARAMETER", "MemberVisibilityCanBePrivate")
    inline fun <reified T> getLast(text: SensibleSpannableStringBuilder, kind: Class<T>): T? =
            text.getAllSpansWithType<T>().lastOrNull()

    inline fun <reified T> end(text: SensibleSpannableStringBuilder, kind: Class<T>,
                               repl: Any) {
        val len = text.length

        val obj = getLast(text, kind)
        val where = text.getSpanStart(obj)

        text.removeSpan(obj)

        if (where != len) {
            text.setSpan(repl, where, len, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
        }
    }

    private fun endQuote(text: SensibleSpannableStringBuilder) {
        // Don't want end newlines inside block
        removeLastNewlines(text)

        val len = text.length
        val obj = getLast(text, Blockquote::class.java)
        val where = text.getSpanStart(obj)

        text.removeSpan(obj)

        if (where != len) {
            // Set quote span
            text.setSpan(MyQuoteSpan(mAccentColor, mQuoteGapWidth, mQuoteStripeWidth), where, len,
                    Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
            // Be slightly smaller
            text.setSpan(RelativeSizeSpan(0.8f), where, len,
                    Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
            // And have background color
            //            text.setSpan(new BackgroundColorSpan(Color.DKGRAY), where, len,
            //                    Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
        }
    }

    @Throws(SAXException::class)
    override fun endElement(uri: String, localName: String, qName: String) {
        handleEndTag(localName)
    }

    private fun handleEndTag(tag: String) {
        when {
            tag.equals("br", ignoreCase = true) -> handleBr(spannableStringBuilder)
            tag.equals("p", ignoreCase = true) -> handleP(spannableStringBuilder)
            tag.equals("div", ignoreCase = true) -> handleP(spannableStringBuilder)
            tag.equals("strong", ignoreCase = true) -> end(spannableStringBuilder, Bold::class.java,
                    StyleSpan(Typeface.BOLD))
            tag.equals("b", ignoreCase = true) -> end(spannableStringBuilder, Bold::class.java,
                    StyleSpan(Typeface.BOLD))
            tag.equals("em", ignoreCase = true) -> end(spannableStringBuilder, Italic::class.java,
                    StyleSpan(Typeface.ITALIC))
            tag.equals("cite", ignoreCase = true) -> end(spannableStringBuilder, Italic::class.java,
                    StyleSpan(Typeface.ITALIC))
            tag.equals("dfn", ignoreCase = true) -> end(spannableStringBuilder, Italic::class.java,
                    StyleSpan(Typeface.ITALIC))
            tag.equals("i", ignoreCase = true) -> end(spannableStringBuilder, Italic::class.java,
                    StyleSpan(Typeface.ITALIC))
            tag.equals("big", ignoreCase = true) -> end(spannableStringBuilder, Big::class.java,
                    RelativeSizeSpan(1.25f))
            tag.equals("small", ignoreCase = true) -> end(spannableStringBuilder, Small::class.java,
                    RelativeSizeSpan(0.8f))
            tag.equals("font", ignoreCase = true) -> endFont(spannableStringBuilder)
            tag.equals("blockquote", ignoreCase = true) -> {
                endQuote(spannableStringBuilder)
                handleP(spannableStringBuilder)
            }
            tag.equals("tt", ignoreCase = true) -> end(spannableStringBuilder, Monospace::class.java,
                    TypefaceSpan("monospace"))
            tag.equals("a", ignoreCase = true) -> endA(spannableStringBuilder)
            tag.equals("u", ignoreCase = true) -> end(spannableStringBuilder, Underline::class.java, UnderlineSpan())
            tag.equals("sup", ignoreCase = true) -> end(spannableStringBuilder, Super::class.java, SuperscriptSpan())
            tag.equals("sub", ignoreCase = true) -> end(spannableStringBuilder, Sub::class.java, SubscriptSpan())
            tag.length == 2 &&
                    Character.toLowerCase(tag[0]) == 'h' &&
                    tag[1] >= '1' && tag[1] <= '6' -> {
                handleP(spannableStringBuilder)
                endHeader(spannableStringBuilder)
            }
            tag.equals("img", ignoreCase = true) -> endImg(spannableStringBuilder)
            tag.equals("ul", ignoreCase = true) -> endUl(spannableStringBuilder)
            tag.equals("ol", ignoreCase = true) -> endOl(spannableStringBuilder)
            tag.equals("li", ignoreCase = true) -> endLi(spannableStringBuilder)
            tag.equals("pre", ignoreCase = true) -> endPre(spannableStringBuilder)
            tag.equals("code", ignoreCase = true) -> endCode(spannableStringBuilder)
            tag.equals("iframe", ignoreCase = true) -> endIframe(spannableStringBuilder)
            tag.equals("tr", ignoreCase = true) -> startEndTableRow(spannableStringBuilder)
            tag.equals("table", ignoreCase = true) -> startEndTable(spannableStringBuilder)
            tag.toLowerCase() in ignoredTags -> ignoreCount--
            else -> endUnknownTag(tag, spannableStringBuilder)
        }
    }

    /**
     * Remove the last newlines from the string, don't want them inside this span
     *
     * @param text spannablestringbuilder
     */
    private fun removeLastNewlines(text: SensibleSpannableStringBuilder) {
        var len = text.length
        while (len >= 1 && text[len - 1] == '\n') {
            text.delete(len - 1, len)
            len = text.length
        }
    }

    private fun handleBr(text: SensibleSpannableStringBuilder) {
        ensureSingleNewline(text)
    }

    private fun endFont(text: SensibleSpannableStringBuilder) {
        val len = text.length
        val obj = getLast(text, Font::class.java)
        val where = text.getSpanStart(obj)

        text.removeSpan(obj)

        if (where != len) {
            val f: Font? = obj

            if (!TextUtils.isEmpty(f!!.mColor)) {
                if (f.mColor.startsWith("@")) {
                    val res = Resources.getSystem()
                    val name = f.mColor.substring(1)
                    val colorRes = res.getIdentifier(name, "color", "android")
                    if (colorRes != 0) {
                        @Suppress("DEPRECATION")
                        val colors = res.getColorStateList(colorRes)
                        text.setSpan(TextAppearanceSpan(null, 0, 0, colors, null), where, len,
                                Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
                    }
                }
            }

            if (f.mFace != null) {
                text.setSpan(TypefaceSpan(f.mFace), where, len,
                        Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
            }
        }
    }

    private fun endA(text: SensibleSpannableStringBuilder) {
        val len = text.length
        val obj = getLast(text, Href::class.java)
        val where = text.getSpanStart(obj)

        text.removeSpan(obj)

        if (where != len) {
            val h: Href? = obj

            h?.mHref?.let { link ->
                text.setSpan(URLSpanWithListener(link, urlClickListener), where, len,
                        Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
            }
        }
    }

    private fun endHeader(text: SensibleSpannableStringBuilder) {
        var len = text.length
        val obj = getLast(text, Header::class.java)

        val where = text.getSpanStart(obj)

        text.removeSpan(obj)

        // Back off not to change only the text, not the blank line.
        while (len > where && text[len - 1] == '\n') {
            len--
        }

        if (where != len) {
            val h: Header? = obj

            text.setSpan(RelativeSizeSpan(HEADER_SIZES[h!!.mLevel]), where,
                    len, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
            text.setSpan(StyleSpan(Typeface.BOLD), where, len,
                    Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
        }
    }

    private fun startEndTable(text: SensibleSpannableStringBuilder) {
        ensureDoubleNewline(text)
    }

    private fun startEndTableRow(text: SensibleSpannableStringBuilder) {
        ensureSingleNewline(text)
    }

    private fun startTableCol(text: SensibleSpannableStringBuilder) {
        ensureSingleNewline(text)
    }

    private fun endImg(text: SensibleSpannableStringBuilder) {
        if (ignoredImage) {
            ignoredImage = false
            return
        }
        ensureDoubleNewline(text)
    }

    private fun endUl(text: SensibleSpannableStringBuilder) {
        val obj = getLast(text, Listing::class.java)
        text.removeSpan(obj)
    }

    private fun endOl(text: SensibleSpannableStringBuilder) {
        val obj = getLast(text, Listing::class.java)
        text.removeSpan(obj)
    }

    private fun endLi(text: SensibleSpannableStringBuilder) {
        val len = text.length
        val obj = getLast(text, Bullet::class.java)
        val where = text.getSpanStart(obj)

        text.removeSpan(obj)

        if (where != len) {
            val offset = 60
            val span: Any = if (obj is CountBullet) {
                // Numbered
                LeadingMarginSpan.Standard(offset, offset)
            } else {
                // Bullet points
                BulletSpan(offset, Color.GRAY)
            }

            text.setSpan(span, where, len, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
        }
        // Add newline
        text.append("\n")
    }

    private fun endPre(text: SensibleSpannableStringBuilder) {
        // yes, take len before appending
        val len = text.length
        ensureDoubleNewline(text)

        val obj = getLast(text, Pre::class.java)
        val where = text.getSpanStart(obj)

        text.removeSpan(obj)

        if (where != len) {
            // TODO
            // Make sure text does not wrap.
            // No easy solution exists for this
            text.setSpan(AlignmentSpan.Standard(Layout.Alignment
                    .ALIGN_NORMAL), where, len,
                    Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
        }

        respectFormatting--

        if (respectFormatting < 0) {
            respectFormatting = 0
        }
    }

    private fun endCode(text: SensibleSpannableStringBuilder) {
        val len = text.length
        val obj = getLast(text, Code::class.java)
        val where = text.getSpanStart(obj)

        text.removeSpan(obj)

        if (where != len) {
            // Want it to be monospace
            text.setSpan(TypefaceSpan("monospace"), where, len,
                    Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
            // Be slightly smaller
            text.setSpan(RelativeSizeSpan(0.8f), where, len,
                    Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
            // And have background color
            text.setSpan(BackgroundColorSpan(Color.DKGRAY), where, len,
                    Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
        }
    }

    private fun endIframe(text: SensibleSpannableStringBuilder) {

    }

    private fun endUnknownTag(tag: String, text: SensibleSpannableStringBuilder) {
        // Override me
    }

    @Throws(SAXException::class)
    override fun characters(ch: CharArray, start: Int, length: Int) {
        if (ignoreCount > 0) {
            return
        }
        val sb = StringBuilder()

        /*
         * Ignore whitespace that immediately follows other whitespace;
         * newlines count as spaces.
         */

        (0 until length)
                .asSequence()
                .map { ch[it + start] }
                .forEach {
                    if (respectFormatting < 1 && it.isWhitespace()) {
                        val prev: Char = if (sb.isEmpty()) {
                            val len = spannableStringBuilder.length

                            if (len == 0) {
                                '\n'
                            } else {
                                spannableStringBuilder[len - 1]
                            }
                        } else {
                            sb.last()
                        }

                        if (!prev.isWhitespace()) {
                            sb.append(' ')
                        }
                    } else {
                        sb.append(it)
                    }
                }

        spannableStringBuilder.append(sb)
    }

    @Throws(SAXException::class)
    override fun ignorableWhitespace(ch: CharArray, start: Int, length: Int) {
    }

    @Throws(SAXException::class)
    override fun processingInstruction(target: String, data: String) {
    }

    @Throws(SAXException::class)
    override fun skippedEntity(name: String) {
    }

    protected class Bold

    protected class Italic

    protected class Underline

    protected class Big

    protected class Small

    protected class Monospace

    protected class Blockquote

    protected class Super

    protected class Sub

    class Listing(var ordered: Boolean) {
        var number: Int = 0

        init {
            number = 1
        }
    }

    protected open class Bullet

    protected class CountBullet : Bullet()

    protected class Pre

    protected class Code

    protected class Font(var mColor: String, var mFace: String?)

    protected class Href(var mHref: String?)

    protected class Header(var mLevel: Int)

    companion object {

        protected val HEADER_SIZES = floatArrayOf(1.5f, 1.4f, 1.3f, 1.2f, 1.1f, 1f)

        private fun ensureDoubleNewline(text: SensibleSpannableStringBuilder) {
            val len = text.length
            // Make sure it has spaces before and after
            if (len >= 1 && text[len - 1] == '\n') {
                if (len >= 2 && text[len - 2] != '\n') {
                    text.append("\n")
                }
            } else if (len != 0) {
                text.append("\n\n")
            }
        }

        private fun ensureSingleNewline(text: SensibleSpannableStringBuilder) {
            val len = text.length
            if (len >= 1 && text[len - 1] == '\n') {
                return
            }
            if (len != 0) {
                text.append("\n")
            }
        }
    }
}
