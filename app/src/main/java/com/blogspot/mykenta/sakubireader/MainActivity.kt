package com.blogspot.mykenta.sakubireader

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.support.v4.app.Fragment
import android.support.v4.app.FragmentManager
import android.support.v4.app.FragmentPagerAdapter
import android.support.v4.app.FragmentStatePagerAdapter
import android.support.v4.view.PagerAdapter
import android.support.v4.view.ViewPager
import android.support.v7.app.AppCompatActivity
import android.support.v7.widget.Toolbar
import android.view.*
import android.webkit.WebView
import android.webkit.WebViewClient
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.jsoup.nodes.Element
import java.nio.charset.Charset

fun getCopyWithoutChildrenSections(element: Element) : Element {
    val copy = element.clone()
    copy.children().forEach {
        if (it.tagName() == "section") it.remove()
    }
    return copy
}
var colouring = true

class MainActivity : AppCompatActivity() {

    /**
     * The [android.support.v4.view.PagerAdapter] that will provide
     * fragments for each of the sections. We use a
     * [FragmentPagerAdapter] derivative, which will keep every
     * loaded fragment in memory. If this becomes too memory intensive, it
     * may be best to switch to a
     * [android.support.v4.app.FragmentStatePagerAdapter].
     */
    private var mSectionsPagerAdapter: SectionsPagerAdapter? = null
    private var bundleOfIDS: Bundle? = null

    /**
     * The [ViewPager] that will host the section contents.
     */
    private var mViewPager: ViewPager? = null

    fun getStyle(): String {
        val input_stream = this.assets.open("style.css")
        val size = input_stream.available()
        val buffer = ByteArray(size)
        input_stream.read(buffer)
        input_stream.close()
        return String(buffer, Charset.forName("UTF-8"))
    }

    fun getSakubiHTML(): String {
        val input_stream = this.assets.open("sakubi.html")
        val size = input_stream.available()
        val buffer = ByteArray(size)
        input_stream.read(buffer)
        input_stream.close()
        return String(buffer, Charset.forName("UTF-8"))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val document = Jsoup.parse(getSakubiHTML())
        val toolbar = findViewById(R.id.toolbar) as Toolbar
        setSupportActionBar(toolbar)

        // Create the adapter that will return a fragment for each of the three
        // primary sections of the activity.

        bundleOfIDS = getIDSBundle(document)
        mSectionsPagerAdapter = SectionsPagerAdapter(supportFragmentManager, document, getStyle(), bundleOfIDS!!)

        // Set up the ViewPager with the sections adapter.
        mViewPager = findViewById(R.id.container) as ViewPager
        mViewPager!!.adapter = mSectionsPagerAdapter

    }

    fun  getIDSBundle(document: Document): Bundle {
        val result = Bundle()
        val sections = document.getElementsByTag("section")
        sections.forEachIndexed { index, element ->
            val clone = getCopyWithoutChildrenSections(element)
            val ids = clone.getElementsByAttribute("id")
            ids.forEach {
                result.putInt(it.attr("id"), index + 1)
            }
        }
        return result
    }


    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        // Inflate the menu; this adds items to the action bar if it is present.
        menuInflater.inflate(R.menu.menu_main, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        val id = item.itemId


        if (id == R.id.action_toc) {
            (mViewPager as ViewPager).currentItem = bundleOfIDS!!.getInt("toc")
            return true
        }
        if (id == R.id.action_toggle_coloring) {
            colouring = !colouring
            mSectionsPagerAdapter!!.notifyDataSetChanged()
        }

        if (id == R.id.action_repo) {
            val browserIntent = Intent(Intent.ACTION_VIEW, Uri.parse("https://github.com/kentaromiura/SakubiReader"))
            startActivity(browserIntent)
        }
        return super.onOptionsItemSelected(item)
    }

    /**
     * A placeholder fragment containing a simple view.
     */
    class PlaceholderFragment : Fragment() {

        override fun onCreateView(inflater: LayoutInflater?, container: ViewGroup?,
                                  savedInstanceState: Bundle?): View? {
            val rootView = inflater!!.inflate(R.layout.fragment_main, container, false)
            val webview = rootView.findViewById(R.id.webview) as WebView
            webview.loadUrl("about:blank")
            val style = arguments.getString(ARG_STYLE)
            webview.loadDataWithBaseURL("file:///android_asset/", "<style>$style</style>" + arguments.getString(ARG_TEXT) + "<br /><br /><br />", "text/html", "utf-8", null)

            webview.setWebViewClient(object: WebViewClient() {
                override fun shouldOverrideUrlLoading(view: WebView?, url: String?): Boolean {
                    if (url != null && url.startsWith("findid://")) {
                        val urlData = url.substring("findid://".length)

                        val page = arguments.getBundle(ARG_IDS).getInt(urlData)
                        (container as ViewPager).currentItem = page
                        return true
                    } else {
                        if (url != null) {
                            val browserIntent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
                            startActivity(browserIntent)
                            return true;
                        }
                    }

                    return super.shouldOverrideUrlLoading(view, url as String?)
                }
            })
            return rootView
        }

        companion object {
            /**
             * The fragment argument representing the section number for this
             * fragment.
             */
            private val ARG_SECTION_NUMBER = "section_number"
            private val ARG_TEXT = "text"
            private val ARG_STYLE = "style"
            private val ARG_IDS = "ids"

            fun getSection(element: Element): String {

                if (colouring) {
                    val tmp = Document("")
                    tmp.appendChild(element)
                    tmp.outputSettings(Document.OutputSettings().prettyPrint(false))

                    return element.html()
                            .replace(Regex("[^ァ-ン\u3400-\u4DB5\u4E00-\u9FCB\uF900-\uFA6A><！a-zA-Z:0-9\"',.()!\\-\\s]+"), {
                              when(it.value) {
                                  "に" -> "<b class=ni>に</b>"
                                  "へ" -> "<b class=he>へ</b>"
                                  "から" -> "<b class=kare>から</b>"
                                  "でした" -> "<b class=copula>でした</b>"
                                  "だった" -> "<b class=copula>だった</b>"
                                  "だ" -> "<b class=copula>だ</b>"
                                  "です" -> "<b class=copula>です</b>"
                                  "の" -> "<b class=possession>の</b>"
                                  "て" -> "<b class=te>て</b>"
                                  "が" -> "<b class=ga>が</b>"
                                  "は" -> "<b class=wa>は</b>"
                                  "を" -> "<b class=o>を</b>"
                                  "か" -> "<b class=ka>か</b>"
                                  "と" -> "<b class=yo>と</b>"
                                  "も" -> "<b class=mo>も</b>"
                                  else -> it.value
                              }
                            })
                            .replace(Regex("[ァ-ン]+"), {
                                "<b class=katakana>${it.value}</b>"
                            })
                            .replace(Regex("[\u3400-\u4DB5\u4E00-\u9FCB\uF900-\uFA6A]+"), {
                                "<b class=kanji>${it.value}</b>"
                            })
                            .replace("href=\"#", "href=\"findid://")
                } else {
                    return element.html()
                            .replace("href=\"#", "href=\"findid://")
                }
            }

            /**
             * Returns a new instance of this fragment for the given section
             * number.
             */
            fun newInstance(sectionNumber: Int, document: Document, style: String, bundleOfIDS: Bundle): PlaceholderFragment {
                val fragment = PlaceholderFragment()
                val args = Bundle()
                args.putInt(ARG_SECTION_NUMBER, sectionNumber)
                args.putString(ARG_STYLE, style)
                if (sectionNumber == 1) {
                    val children = document.body().children()
                    args.putString(ARG_TEXT, "<h1>${children[0].html()}</h1><p>${children[1].html()}</p><p>${children[2].html()}</p>" )
                } else {
                    val copy = getCopyWithoutChildrenSections(document.getElementsByTag("section")[sectionNumber - 2])
                    args.putString(ARG_TEXT, getSection(copy))
                }
                args.putBundle(ARG_IDS, bundleOfIDS)
                fragment.arguments = args
                return fragment
            }
        }
    }

    /**
     * A [FragmentPagerAdapter] that returns a fragment corresponding to
     * one of the sections/tabs/pages.
     */
    inner class SectionsPagerAdapter(fm: FragmentManager, val document: Document, val style: String, val bundleOfIDS: Bundle) : FragmentStatePagerAdapter(fm) {

        override fun getItemPosition(`object`: Any?): Int {
            // needed to refresh view when toggle colours is used.
            return PagerAdapter.POSITION_NONE
        }

        override fun getItem(position: Int): Fragment {
            // getItem is called to instantiate the fragment for the given page.
            // Return a PlaceholderFragment (defined as a static inner class below).
            return PlaceholderFragment.newInstance(position + 1, document, style, bundleOfIDS)
        }

        override fun getCount(): Int {
            // Show 3 total pages.

            val sections = document.getElementsByTag("section")

            return sections.size + 1 // sections + initial view
        }

        override fun getPageTitle(position: Int): CharSequence? {
            return "SECTION $position"
        }
    }
}
