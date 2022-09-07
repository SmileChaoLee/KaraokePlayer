package exoplayer

import com.smile.karaokeplayer.BaseApplication
import com.facebook.ads.AudienceNetworkAds
import com.smile.smilelibraries.facebook_ads_util.FacebookInterstitial
import com.smile.smilelibraries.show_interstitial_ads.ShowInterstitial

private const val TAG: String = "SmileApplication"

class SmileApplication : BaseApplication() {
    override fun setGoogleAdMobAndFacebookAudioNetwork() {
        AudienceNetworkAds.initialize(this)
        facebookInterstitialID = "1712962715503258_1712963252169871"
        facebookInterstitialID = testString + facebookInterstitialID
        facebookInterstitial = FacebookInterstitial(applicationContext, facebookInterstitialID)
        facebookBannerID = testString + "1712962715503258_2019623008170559"
        googleAdMobAppID = "ca-app-pub-8354869049759576~5549171584"
        googleAdMobInterstitialID = "ca-app-pub-8354869049759576/1418354889"
        googleAdMobBannerID = "ca-app-pub-8354869049759576/8267060571"
        googleAdMobNativeID = "ca-app-pub-8354869049759576/7985456524"
        AdProvider = ShowInterstitial.FacebookAdProvider
    }
}