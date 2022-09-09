package videoplayer

import com.smile.karaokeplayer.BaseApplication
import com.smile.smilelibraries.show_interstitial_ads.ShowInterstitial

class SmileApplication : BaseApplication() {
    override fun setGoogleAdMobAndFacebookAudioNetwork() {
        // Token: EAAEN04aiEXUBAHBQwZBZB4gmWOueuRTEZCHMCAWOHZBB7hjavIuXgKELNvtfcIGCJV72zkohipkSZALG51WCXX6xbN3oUy84i8NrIvuc44RTfypgdcyOTnaPyM1W4JZBjQvDGuWsCRGmbusWaZCKmWt5iCkhQklOKeZC4Edx6FDIGTUUiHMKURzS
        // App ID: 633653050588487
        // facebookInterstitialID = "296677124739445_296678328072658";
        // facebookBannerID = testString + "296677124739445_296687284738429";
        googleAdMobAppID = "ca-app-pub-8354869049759576~5376732060"
        googleAdMobInterstitialID = "ca-app-pub-8354869049759576/7715939032"
        googleAdMobBannerID = "ca-app-pub-8354869049759576/2158051096"
        googleAdMobNativeID = "ca-app-pub-8354869049759576/6498242044"
        AdProvider = ShowInterstitial.GoogleAdMobAdProvider
    }
}