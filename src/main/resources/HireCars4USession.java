package com.alltria.hirecars4u.web.common;

import com.alltria.affiliates.Affiliate;
import com.alltria.affiliates.AffiliateManager;
import com.alltria.bcrs.adapters.BCRSAPI.OnSaleType;
import com.alltria.bcrs.businessObjects.*;
import com.alltria.bcrs.businessObjects.Date;
import com.alltria.bcrs.businessObjects.response.AmendResponse;
import com.alltria.bcrs.utility.DateUtils;
import com.alltria.core.communication.CookieData;
import com.alltria.core.content.ContentMessageResources;
import com.alltria.core.content.ContentMessageResourcesFactory;
import com.alltria.core.discounts.Discount;
import com.alltria.core.discounts.DiscountManager;
import com.alltria.core.discounts.Rule;
import com.alltria.core.discounts.RuleList;
import com.alltria.core.events.SessionLifespan;
import com.alltria.core.geoip.GeoIP;
import com.alltria.core.locale.LocaleUtilsManager;
import com.alltria.core.owf.OneWayFee;
import com.alltria.core.switcher.Switcher;
import com.alltria.core.systemParameters.NodeParameters;
import com.alltria.core.systemParameters.SystemParameter;
import com.alltria.core.systemParameters.SystemParameterManager;
import com.alltria.core.thread.ThreadLocalUtils;
import com.alltria.core.thread.ThreadLocalUtils.ABTest;
import com.alltria.core.web.common.Session;
import com.alltria.core.web.common.SessionRequestMapping;
import com.alltria.hirecars4u.utils.LocaleUtils;
import com.alltria.hirecars4u.web.booking.quote.CustomerPersonalInformation;
import com.alltria.hirecars4u.web.common.domain.LoyaltyHub;
import com.alltria.hirecars4u.web.search.PricelineOpaqueCarResult;
import com.alltria.hirecars4u.web.search.SearchOption;
import com.alltria.hirecars4u.web.search.StoredSearchIFace;
import com.rentalcars.cache.CacheApi;
import com.rentalcars.cache.Cacheable;
import com.rentalcars.cookie.policy.CookiePolicy;
import com.rentalcars.crm.backingObjects.Login;
import com.rentalcars.promotion.domain.CustomerPromotion;
import com.rentalcars.threedsecure.domain.ThreedsAuthCheckResult;
import com.rentalcars.threedsecure.domain.ThreedsEnrollmentCheckResult;
import com.rentalcars.utils.SerializableUtils;
import com.rentalcars.web.crm.oauth.OauthDetails;
import com.rentalcars.web.events.events.ActionEvent;
import com.rentalcars.web.payment.threeds.domain.ThreeDSecureRequestData;
import com.traveljigsaw.util.*;
import com.traveljigsaw.util.competitorProxy.CompetitorProxyInfo;
import com.traveljigsaw.util.competitorProxy.CompetitorProxyLookup;
import com.traveljigsaw.util.competitorProxy.ProxyType;
import com.traveljigsaw.util.hostname.HostnameResolveListener;
import com.traveljigsaw.web.channel.ChannelMappingManager;
import com.traveljigsaw.web.customerData.CustomerDataCookie;
import com.traveljigsaw.web.device.Device;
import com.traveljigsaw.web.device.DeviceManager;
import com.traveljigsaw.web.event.EventLoggerUtils;
import com.traveljigsaw.web.experiment.ExperimentCookie;
import com.traveljigsaw.web.experiment.ExperimentManager;
import com.traveljigsaw.web.experiment.PricingExperimentCookie;
import com.traveljigsaw.web.filter.ABTestFilter;
import com.traveljigsaw.web.filter.ConfigureSessionFilter;
import com.traveljigsaw.web.filter.RequestThreadLocalFilter;
import com.traveljigsaw.web.language.Language;
import com.traveljigsaw.web.language.LanguageManager;
import com.traveljigsaw.web.livechat.model.LiveChatSession;
import com.traveljigsaw.web.mailClickData.MailClickData;
import com.traveljigsaw.web.redirect.RedirectManager;
import com.traveljigsaw.web.redirect.RedirectRule;
import com.traveljigsaw.web.seo.experiment.LandingData;
import com.traveljigsaw.web.traffic.Traffic;
import com.traveljigsaw.web.traffic.TrafficClassification;
import com.traveljigsaw.web.traffic.TrafficManager;
import com.traveljigsaw.web.ydf.SearchResult;
import lombok.Getter;
import lombok.Setter;
import net.sf.uadetector.ReadableUserAgent;
import net.sf.uadetector.UserAgentStringParser;
import net.sf.uadetector.service.UADetectorServiceFactory;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang.BooleanUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.math.NumberUtils;
import org.apache.struts.util.ModuleUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.ServletContext;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.Serializable;
import java.net.URI;
import java.net.URISyntaxException;
import java.text.SimpleDateFormat;
import java.util.*;

@SuppressWarnings({"rawtypes","unchecked"})
public class HireCars4USession extends SessionRequestMapping implements HostnameResolveListener, Serializable {

	//TODO Pull this out into a test, once CI is properly setup
	static {
		if(!SerializableUtils.isSerializable(com.alltria.hirecars4u.web.common.HireCars4USession.class)) {
			throw new RuntimeException("HireCars4USession is not serializable, the webapp cannot start.");
		}
	}

	private static final long serialVersionUID = 5473191435812374929L;
	public static final String[] POST_TRAP_NAMES = { "promoCountry", "promoCity", "promoLoc", "promoAge" };
	public static final String COMPETITOR_COOKIE_NAME = "compcode";
	public static final String COMPETITOR_AFFILIATE_COOKIE_NAME = "compaffcode";
	public static final String COR_LABLE_NAME = "tjcor";
	public static final String CURRENCY_LABLE_NAME = "tj_pref_currency";
	public static final String CONF_COOKIE_NAME = "tj_conf";
	public static final String LANGUAGE_LABEL_ALT = "tj_lang";
	public static final String GOOGLE_CUSTOMERID_COOKIE_NAME = "__utma";
	public static final String CRM_COOKIE_NAME = "rccrm";
	public static final String TRADEDOUBLER_COOKIE_NAME = "tduid";

	public static final String CANNOT_FIND_LANGUAGE_MESSAGE_KEY = "common.error.cannotFindLocale";

    private static final String CODE_OF_COUNTRY_OF_RESIDENCE_INVALID = "19";

    private String vehicleInfoListCacheKey;
    private ArrayList<String> vehicleInfoListCacheKeyList = new ArrayList<String>();
	private Serializable abTests;
    private ArrayList<String> activeExperimentNames;
    private boolean configuredForABTests;
	private SearchResult youngDriverFee;
    private OneWayFee oneWayFee;
    private String BookingFormData=null;
	private int sendtolist=0;
	private int includeinsummary=0;
	private String VehicleType="1";
	private boolean affilateCodeOnURL=false;
	private boolean locationsNotComplete=true;
	private String competitorDiscountName=null;
	private String competitorAffiliateDiscountName=null;
	private boolean OnWrongSite=false;
	private String AffiliatePage="";
	private ArrayList preTrackedSearches=new ArrayList();
	private static SimpleDateFormat SDF=new SimpleDateFormat(DateUtils.DMYYYY);
	private ArrayList DateList=new ArrayList();
	private static transient Logger LOG = LoggerFactory.getLogger(com.alltria.hirecars4u.web.common.HireCars4USession.class);
	private String promoCode="";
	private String entryPage = "";
	private String google_lid = "";
	private String google_cgid = "";
	private int timeOffset=0;
	private boolean localuser=false;
	private boolean loadedImages=true;
	private boolean CompEmailSent=false;
	private boolean clickthroughCounted = false;
	private String serverName = "";
	private String serverPort = "";
	private String integrationMethod = "";

	private String freeTextSearchServerName = "";
	private String freeTextSearchServerPort = "";

	private String sourceMarket = "";

    private String pickupLocation = "";
    private String dropoffLocation = "";

	private String affiliateGroup = "";
	private String affiliatePassword = "";
	private boolean affiliateMasterLogin = false;
	private String affiliateCompanyName = "";
	private boolean affiliateExists = true;

	private String languageIso = null;
	private String languageIsoUnmapped = "";
	private String altLang; // language which customer chose explicitly


	private ArrayList searchOptions = new ArrayList();

	private boolean competitor = false;
	private boolean isPossibleCompetitor = false;
	private boolean captchaRequired = false;
	private boolean captchaPassed = false;
	private boolean ipCompetitor=false;
	private boolean lookedUpProxy = false;
	private ProxyType proxyType = null;
	private boolean denyAccess = false;
	private ArrayList countrySearches=new ArrayList();
	private boolean allSearches = false;
	private boolean DepositSearchCompleted=false;
	String ReasonString="";
	private String reasonExtraInfo = "";
	private String hostname = "";
	private String applicationModule = null;
	private String connectionId;
	private String languageApplication = null;
	private HashSet<String> languageAppOptions = null;
	private String RequestURL="";
	private boolean cookiePopupViewed = false;
	private String userAgentString;
	private String customerType = null;
	private String entryUrl = null;
	private String entryQueryString = null;

	private boolean testPrice = false;
	private Boolean gridViewLayout = null;
	private boolean searchAgain = false;
	private boolean autoOptInAbg = false;
	private String previousRedirect = "";
	@Setter
	private PPCInfo ppcInfo;
	@Setter @Getter
	private CustomerPersonalInformation customerPersonalInformation;

	/*
	 * Created to retrive session ID in PreAuthUtil.java
	 * for PreAuthEventImpl
	 */
	private String sessionID;

	private ArrayList<String[]> postTrapPositives = new ArrayList<>();
	private ArrayList<String[]> postTrapNegatives = new ArrayList<>();

	public static final String[] TRACKING_DATA_LABELS = {HireCars4UAction.ADCAMP_LABEL,
														HireCars4UAction.ADCOUNTRY_LABEL,
														HireCars4UAction.ADPLAT_LABEL,
														HireCars4UAction.AFFILIATECODE_LABEL,
														HireCars4UAction.CREATIVEID_MD5_LABEL,
														HireCars4UAction.PPC_INFO_PLACEMENT_LABEL,
														HireCars4UAction.PPC_INFO_TARGET_LABEL,
														HireCars4UAction.PPC_INFO_PARAM1_LABEL,
														HireCars4UAction.PPC_INFO_PARAM2_LABEL,
														HireCars4UAction.PPC_INFO_ACEID_LABEL,
														HireCars4UAction.PPC_INFO_ADPOSITION_LABEL,
														HireCars4UAction.PPC_INFO_NETWORK_LABEL,
														HireCars4UAction.PPC_INFO_FEEDITEMID_LABEL,
														HireCars4UAction.PPC_INFO_TARGETID_LABEL,
														HireCars4UAction.PPC_INFO_LOCPHYSICALMS_LABEL,
														HireCars4UAction.PPC_INFO_LOCINTERESTMS_LABEL,
														HireCars4UAction.PPC_INFO_DEVICE_LABEL,
														HireCars4UAction.PPC_INFO_DEVICEMODEL_LABEL};


	private HashMap<String, String> serverSideCookies = new HashMap<String, String>();
	public static final String TRACKING_COOKIE_NAME = "tj_track";
	public static final String PRICING_EXPERIMENT_COOKIE_NAME = "tj_pe_exp";
	private PricingExperimentCookie pricingExperiments = null;
	private String pricingExperimentsString;

	public static final String[] CONFIGURATION_DATA_LABELS = {COR_LABLE_NAME,
															LocaleUtilsManager.LANGUAGE_LABEL,
															CURRENCY_LABLE_NAME};
	private int preAuthRetries = -2;
	private boolean authSuccess = true;
	private boolean payLocal = false;

	@Getter
	@Setter
	private ThreedsEnrollmentCheckResult threedsEnrollmentCheckResult;

	@Getter
	@Setter
	private ThreeDSecureRequestData threeDSecureRequestData;

	@Getter
    @Setter
	private ThreedsAuthCheckResult threedsAuthCheckResult;

	@Getter
    @Setter
	private int threedsAuthTries = 1;

	private Traffic traffic = null;
	private TrafficClassification trafficClassification = null;
	private ExperimentCookie experimentCookie = null;
	private boolean corPopup = false;
	private boolean geoIpFailed = false;
	private String salesChannel = "";
	private String clickTaleCode = "";

	private String quoteRef = "";

	private boolean affiliateMasterSearch=false;
	private String affiliateLogin="";

	private boolean rateCodeExpired = false;
	private String rateCodeExpirationReason = "";
	private String rateCodeExpirationClass = "";

	private boolean optimiseHtml = true;
	private boolean badPostalCode = false;
	private boolean isReferrerOurSite = true;

	private String refId = "";
	private String refClickId = "";

	private String displayCurrency = "";
	private String baseCurrency = "";
	private String backupBaseCurrency = "";
	private String backupSourceMarket = "";

	private CustomerDataCookie customerDataCookie;
	private StoredSearchIFace storedSearch;

	private String ipAddressOverride = "";
	private boolean ignorePricingExperiments = false;

	private String affiliateHeaderLogo = "";
    private String affiliateHeaderLogoHref="";
	private boolean proAffiliate = false;

	private boolean affShowPhoneNumber = true;
	private boolean affAllowCrossSell = true;
	private boolean blockABTests = false;
	private boolean showFullyOnTests = true;
	private boolean affShowFts = true;
	private boolean isMobile = false;

	private boolean logged = false;
	private String sessionAffiliateCode = "";

	private MailClickData mailClickData;

	// Trade double link tracking ID for affiliates
	private String tduid = "";

	private String paymentToken = "";
	private PaymentInfo tokenCreditCard = null;
	private Integer oldLanguage;

	private Login login = new Login();
	// set to true when a user logs in, remains true if the user then logs out
	private boolean previouslyLoggedIn = false;
	// set to true when a loyal user logs in, remains true if the user then logs out
    private boolean previouslyLoggedInLoyal = false;

    private boolean hasSeenNoResults = false;
    // Set to true if the user has changed language specifically on this request.
    private boolean nowChangedLanguage = false;
    // The previous SEO Category language-instance URL slug that the user was accessing.
    private String seoCategoryPrevSlug;
    // The previous SEO Post language-instance URL slug that the user was accessing.
    private String seoPostPrevSlug;

	////// AMEND Functionality Start  ///////
	private String amendBookingReference = "";

	private int numberOfSearchesMade = 0;

	private String promotionEnabler;

	private ArrayList<CustomerPromotion> customerPromotions = new ArrayList<CustomerPromotion>();

	private int loyaltyLevel = 0;

	private boolean rollingCurl;

    private boolean useSupplierXmlSearch;

	/* Selected vehicle is Avis / Budget group */
	private boolean selectedVehicleABG = false;


	private LandingData landingData;
	private HashSet<String> landingEventSentUrls;

	private String providerPaymentReference = "";

	private VehiclePrice amendSelectedVehiclePrice;
	private VehiclePrice amendInitialVehiclePrice;
	private boolean isLoyaltyControlUser;

	private ArrayList<String> alreadyVisitedCars = new ArrayList<String>();

	private String latestSearchCountryEnglish = "";
	private String latestSearchCityEnglish = "";
	private String latestSearchLocationNameEnglish = "";
	private String latestSearchType = "";

    @Getter
    private boolean ftsWasEnabled;

    @Setter
    @Getter
    //identify when web navigation came from RC-APP.
    private boolean isApp;

    @Getter
    @Setter
    private String firstPageOpened;

    @Getter
    @Setter
    private String openingTimesRevisedTimeForSearch;

    @Getter
    @Setter
    private String openingTimesRequestedTimeForSearch;

    @Getter
    @Setter
    private boolean openingTimesRevisedTime;

    @Getter
    @Setter
    private boolean leadTimeSearchPerformed;

    private LiveChatSession liveChatSession;

    @Getter
	@Setter
    private boolean awsRoutingExperimentProcessed = false;
	private transient CookiePolicy cookiePolicy;

	public VehiclePrice getAmendInitialVehiclePrice() {
		return amendInitialVehiclePrice;
	}

	public void setAmendInitialVehiclePrice(VehiclePrice amendInitialVehiclePrice) {
		this.amendInitialVehiclePrice = amendInitialVehiclePrice;
	}

	public VehiclePrice getAmendSelectedVehiclePrice() {
		return amendSelectedVehiclePrice;
	}

	public void setAmendSelectedVehiclePrice(VehiclePrice amendSelectedVehiclePrice) {
		this.amendSelectedVehiclePrice = amendSelectedVehiclePrice;
	}

	private BookingList crmBookings = null;
	private Boolean useCommonAffTilesDefs = null;

	private HashMap<String, String> urlParams;

	private double lowestPriceFromLatestSearch = 0d;
	private String cheapestCarGroupFromLatestSearch = "";
	private String lastViewedCarIndex = "";

	@Getter
	@Setter
	private boolean corCurrencyAligned = true;

    private String countryOfOrigin = "";
    private boolean bookingJustMade = false;
    private LoyaltyHub loyaltyHub;
    private String enabledSecretDealBetterThanBestPrice;
    private String enabledSecretDealClosedUserGroup;
    private OauthDetails oauthDetails = new OauthDetails();
	@Getter @Setter
	private boolean fingerPrintEventSent;

	@Getter @Setter private PartnerRewards partnerRewards = new PartnerRewards();
	@Getter @Setter private SSOMemberInfo ssoMemberInfo = new SSOMemberInfo();
	@Getter @Setter private SSOSignInDetails ssoSignInDetails = new SSOSignInDetails();
	@Getter @Setter private MetaData metaData = new MetaData();
	@Getter @Setter private MetaData metaDataBook = new MetaData();

	@Setter
    LinkedHashMap<String, MemberLoyaltyInfo> memberLoyaltyInfoList;


    // When we scrape the priceline popunder store the results on the session,
    // so we do not have to keep scraping the popunder if the user changes page on search results
    private HashMap<String, List<PricelineOpaqueCarResult>> pricelineCachedResults
    	= new HashMap<String, List<PricelineOpaqueCarResult>>();

	@Getter
	@Setter
	private String inpathClient = "";

	@Getter
	@Setter
	private String travelReason = "";

	@Getter
	@Setter
	private boolean localExtrasWhichExistAsPayNowExtrasHaveBeenFilteredOut = false;

	@Getter
	@Setter
	private boolean paymentSurveySubmitted = false;

    public LoyaltyHub getLoyaltyHub() {
		return loyaltyHub;
	}

	public void setLoyaltyHub(LoyaltyHub loyaltyHub) {
		this.loyaltyHub = loyaltyHub;
	}

	public String getCountryOfOrigin() {
        return countryOfOrigin;
    }

    public void setCountryOfOrigin(String countryOfOrigin) {
        this.countryOfOrigin = countryOfOrigin;
    }

	public double getLowestPriceFromLatestSearch() {
		return lowestPriceFromLatestSearch;
	}

	public void setLowestPriceFromLatestSearch(double lowestPriceFromLatestSearch) {
		this.lowestPriceFromLatestSearch = lowestPriceFromLatestSearch;
	}

	public String getCheapestCarGroupFromLatestSearch() {
		return cheapestCarGroupFromLatestSearch;
	}

	public void setCheapestCarGroupFromLatestSearch(String cheapestCarGroupFromLatestSearch) {
		this.cheapestCarGroupFromLatestSearch = cheapestCarGroupFromLatestSearch;
	}

    public void setTestPrice (boolean testPrice) {
		this.testPrice = testPrice;
	}

	public boolean getTestPrice() {
		return testPrice;
	}

	public void setSelectedVehicleABG(boolean selectedVehicleABG) {
		this.selectedVehicleABG = selectedVehicleABG;
	}

	public boolean isSelectedVehicleABG() {
		return selectedVehicleABG;
	}

	public void setRollingCurl(boolean rollingCurl) {
		this.rollingCurl = rollingCurl;
	}

	public boolean isRollingCurl() {
		return rollingCurl;
	}

    public boolean isUseSupplierXmlSearch() {
        return useSupplierXmlSearch;
    }

    public void setUseSupplierXmlSearch(boolean useSupplierXmlSearch) {
        this.useSupplierXmlSearch = useSupplierXmlSearch;
    }

    public void incNumberOfSearchesMade() {
		numberOfSearchesMade++;
	}

	public int getNnumberOfSearchesMade() {
		return numberOfSearchesMade;
	}

    public String getAmendBookingReference() {
		return amendBookingReference;
	}

	public void setAmendBookingReference(String amendBookingReference) {
		this.amendBookingReference = amendBookingReference;
	}

	private boolean skipLangCurrCheck = false;

	public Boolean checkAmendSession(String amendOn)
	{
		 if(amendOn.equals("on") && this.amendBookingReference.equals(""))
         {
         	 this.setAmendError("session");
         	 return false;
         }
		 else
			 return true;
	}

	private double amendBookingAmount = 0.0;

	public double getAmendBookingAmount() {
		return amendBookingAmount;
	}

	public void setAmendBookingAmount(double amendBookingAmount) {
		this.amendBookingAmount = amendBookingAmount;
	}

	private String amendEmail = "";

	public String getAmendEmail() {
		return amendEmail;
	}

	public void setAmendEmail(String amendEmail) {
		this.amendEmail = amendEmail;
	}

	DriverInfo amendDriver = new DriverInfo();

	public DriverInfo getAmendDriver() {
		if(amendDriver.getDriverName()==null)
		{
			amendDriver.setDriverName(new DriverName());
		}
		return amendDriver;
	}

	public void setAmendDriver(DriverInfo amendDriver) {
		this.amendDriver = amendDriver;
	}

	private String amendFlightNo = "";

	public String getAmendFlightNo() {
		return amendFlightNo;
	}

	public void setAmendFlightNo(String amendFlightNo) {
		this.amendFlightNo = amendFlightNo;
	}

	private String amendAdditionalInfo = "";

	public String getAmendAdditionalInfo() {
		return amendAdditionalInfo;
	}

	public void setAmendAdditionalInfo(String amendAdditionalInfo) {
		this.amendAdditionalInfo = amendAdditionalInfo;
	}

	public void clearAmendParameters() {
		this.amendAdditionalInfo = "";
		this.amendDriver = new DriverInfo();
		this.amendDriver.setDriverName(new DriverName());
		this.amendBookingAmount = 0;
		this.amendFlightNo = "";
		//if(this.amendEmail!="error")
		this.amendEmail = "";
		this.amendBookingReference = "";
	}

	private String amendError = "";


	public String getAmendError() {
		return amendError;
	}

	public void setAmendError(String amendError) {
		this.amendError = amendError;
	}

	public void populateAmendFields(Booking booking)
	{
		Calendar c = Calendar.getInstance();
    	c.setTime(booking.getDropOffLoc().getDate().toJavaDate());
    	c.add(Calendar.DATE, 1);
    	AmendResponse response1 = Booking.amendBooking(null, null, null, null, Date.fromTimestamp(c.getTimeInMillis()), null, null, null, booking.getReference(), true);
    	this.setAmendBookingAmount(response1.getTransferAmount());
	   	this.setCountryOfResidence(booking.getCor().getCode().toLowerCase());
	   	this.setBaseCurrency(booking.getPrice().getCurrency());
	   	this.setDisplayCurrency(booking.getPrice().getCurrency());
	   	this.configureToBookingSettings(booking.getSourceMarket(), booking.getPrice().getCurrency());
	   	this.setAmendBookingReference(booking.getReference());
	}

	//////AMEND Functionality END  ///////



	private String pricingEngineRateCodeOverride = "";

	private String plLoyal = "";

	private Device device = Device.DEVICE_GENERIC;
	private String deviceRedirect = RedirectRule.TYPE_NONE;
    private Booking booking;

    private double supplierProtectionCost = 0.0;

//**	-Stored in the cookies instance variable-		**//
//	private String md5 = "";
//	private String creativeId = "";

	private static String getCookieValue(HttpServletRequest request, String cookieName)
	{
		if(request.getCookies()!=null)
		{
			for(Cookie c : request.getCookies())
			{
				if(c.getName().equals(cookieName))
				{
					return c.getValue();
				}
			}
		}
		return null;
	}

	public void setBookingFormData(String BookingFormData)
	{
		this.BookingFormData=BookingFormData;
	}

	public String getBookingFormData()
	{
		return BookingFormData;
	}

	public void setIsMobile(boolean ismobile)
	{
		this.isMobile = ismobile;
	}

	public boolean getIsMobile()
	{
		return isMobile;
	}

	public void setDevice(Device device) {
		this.device = device;
	}

	public Device getDevice() {
		return device;
	}

	public void setDeviceRedirect(String deviceRedirect) {
		this.deviceRedirect = deviceRedirect;
	}

	public String getDeviceRedirect() {
		return deviceRedirect;
	}

	public String getPreviousRedirect() {
		return previousRedirect;
	}

	public void setPreviousRedirect(String previousRedirect) {
		this.previousRedirect = previousRedirect;
	}


	public String getRequestURL()
	{
		return RequestURL;
	}

	public void setRequestURL(String RequestURL)
	{
		try {
			SystemParameterManager spm = SystemParameterManager.getInstance();
			Collection<SystemParameter> fromParams = spm.filterParameters("*", serverName, "default", "default", "entryUrlRewriteFrom", "*");
			Collection<SystemParameter> toParams = spm.filterParameters("*", serverName, "default", "default", "entryUrlRewriteTo", "*");
			for (SystemParameter fp : fromParams) {
				for (SystemParameter tp : toParams) {
					if (fp.getName().equals(tp.getName())) {
						if (RequestURL.matches(fp.getValue())) {
							RequestURL = RequestURL.replaceAll(fp.getValue(), tp.getValue());
							break;
						}
					}
				}
			}
		}
		catch (Exception e) { LOG.warn("Unexpected error", e);}
		this.RequestURL=RequestURL;
	}

	public static String retrieveServerName(HttpServletRequest request, com.alltria.hirecars4u.web.common.HireCars4USession hc4uSession)
	{
		return retrieveServerName(request, hc4uSession.getServerName());
	}

	public static String retrieveServerName(HttpServletRequest request)
	{
		return retrieveServerName(request, "");
	}

	private static String retrieveServerName(HttpServletRequest request, String sessionServerName)
	{
		String serverName = "";
		String requestOverride = HireCars4UForm.escapeHtml(request.getParameter("serverName"));
		String cookieOverride = getCookieValue(request, "serverName");
		if(requestOverride!=null && !"".equals(requestOverride))
		{
			serverName = requestOverride;
			LOG.debug("serverName: " + serverName + " (overridden by request param)");
		}
		else if(cookieOverride!=null && !"".equals(cookieOverride))
		{
			serverName = cookieOverride;
			LOG.debug("serverName: " + serverName + " (overridden by cookie param)");
		}
		else if(sessionServerName!=null &&  !"".equals(sessionServerName))
		{
			serverName = sessionServerName;
			LOG.debug("serverName: " + serverName);
		}
		else
		{
            serverName = request.getServerName();
            if("secure.rentalcars.com".equals(request.getServerName())) {
                serverName = "www.rentalcars.com";
            }
			LOG.debug("serverName: " + serverName);
		}
		if (Boolean.parseBoolean(SystemParameterManager.getInstance().getSystemParameterValue("default", "default", "default", "InputCleaner", "default", "backendServerNameEnabled", "true"))) {
			serverName = serverName.replaceAll("[^A-Za-z0-9\\-\\_\\.]", "");
		}
		return serverName;
	}

	public static String retrieveServerPort(HttpServletRequest request)
	{
		String serverPort = "";
		String requestOverride = HireCars4UForm.escapeHtml(request.getParameter("serverPort"));
		String cookieOverride = getCookieValue(request, "serverPort");
		if(requestOverride!=null && !"".equals(requestOverride))
		{
			serverPort = requestOverride;
			LOG.debug("serverPort: " + serverPort + " (overridden by request param)");
		}
		else if(cookieOverride!=null && !"".equals(cookieOverride))
		{
			serverPort = cookieOverride;
			LOG.debug("serverPort: " + serverPort + " (overridden by cookie param)");
		}
		else
		{
			serverPort = Integer.toString(request.getServerPort());
			LOG.debug("serverName: " + serverPort);
		}
		return serverPort;
	}

	public static String retrieveModule(HttpServletRequest request,
                                        ServletContext servletContext)
	{
		String module = ModuleUtils.getInstance().getModuleName(request, servletContext);
		LOG.debug("module: " + module);
		return module;
	}

	public static String retrieveModuleApplication(String serverName, String module)
	{
		if(module != null && !module.equals(""))
		{
			SystemParameterManager spm = SystemParameterManager.getInstance();
			String moduleApplication = spm.getSystemParameterValue("default",serverName,
					"moduleApplications", "default", "default", module);
			LOG.debug("moduleApplication: " + moduleApplication);
			return moduleApplication;
		}
		else
		{
			return null;
		}
	}

	public static HashSet<String> retrieveLanguageApplicationOptions(String serverName,
                                                                     String moduleApplication)
	{
		SystemParameterManager spm = SystemParameterManager.getInstance();
		String app = null;
		if(moduleApplication!=null && !moduleApplication.equals(""))
		{
			app = moduleApplication;
		}else
		{
			app = serverName;
		}
		Collection<SystemParameter> params = spm.filterParameters("*",app, "default",
				"default", "languageApplications", "*");
		HashSet<String> options = new HashSet<String>();
		for(SystemParameter p : params)
		{
			options.add(p.getValue());
		}
		return options;
	}

	public static String retrieveLanguageApplication(HttpServletRequest request,
                                                     com.alltria.hirecars4u.web.common.HireCars4USession hc4uSession, Set<String> langAppOptions)
	{
		String langApp = null;
		String langAppOverride = HireCars4UForm.escapeHtml(request.getParameter("languageApp"));
		String langAppCookieOveride = getCookieValue(request, "languageApp");
		String currentLang = hc4uSession.getLanguageApplication();

		if(langAppOverride != null && !langAppOverride.equals(""))
		{
			langApp = langAppOverride;
		}
		else if(currentLang!=null && !currentLang.equals("") && langAppOptions.contains(currentLang))
		{
			langApp = currentLang;
		}
		else if(langAppCookieOveride != null && !"".equals(langAppCookieOveride))
		{
			langApp = langAppCookieOveride;
			LOG.debug("languageApplication: [" + langApp + "] (overridden from cookie param)");
		}
		else
		{
			langApp = null;
		}

		return langApp;
	}

	public static String retrieveApplication(HttpServletRequest request,
                                             String serverName, String moduleApplication, String languageApplication)
	{
		return retrieveApplication(request, serverName, moduleApplication,
				languageApplication, null);
	}

	public static String retrieveApplication(HttpServletRequest request,
                                             String serverName, String moduleApplication, String languageApplication,
                                             String override)
	{
		String application = "";

		String requestOverride = HireCars4UForm.escapeHtml(request.getParameter("overrideApplication"));
		String cookieOverride = getCookieValue(request, "overrideApplication");

		if(override != null && !"".equals(override))
		{
			application = override;
			LOG.debug("application: [" + application + "] (overriden from function param)");
		}
		else if(requestOverride != null && !"".equals(requestOverride))
		{
			application = requestOverride;
			LOG.debug("application: [" + application + "] (overridden from request param)");
		}
		else if(cookieOverride != null && !"".equals(cookieOverride))
		{
			application = cookieOverride;
			LOG.debug("application: [" + application + "] (overridden from cookie param)");
		}
		else if(languageApplication != null && !languageApplication.equals(""))
		{
			application = languageApplication;
			LOG.debug("application: [" + application + "] (overidden from language app)");
		}
		else if(moduleApplication != null && !moduleApplication.equals(""))
		{
			application = moduleApplication;
			LOG.debug("application: [" + application + "] (overridden from moduleApp)");
		}
		else
		{
			application = serverName;
			LOG.debug("application: [" + application + "]");
		}
		return application;
	}

	public String toString(){
		StringBuilder sb = new StringBuilder(this.getClass().getName()+"\n");

		sb.append("application:").append(this.getApplication()).append("\n")
		.append("locale:").append(this.getLocale()).append("\n")
		.append("lang:").append(this.getLanguage()).append("\n")
		.append("source market:").append(this.getSourceMarket()).append("\n")
		.append("realVisit:").append(this.isRealVisit()).append("\n");

		return sb.toString();
	}

	private String getCorFromLang(String lang){

		if(lang.matches("au|bg|br|de|ee|es|fi|fr|gr|hr|hu|id|ie|is|it|lt|lv|my|nl|no|nz|ph|pl|pt|ro|rs|ru|sk|th|tr|us|za")) return lang;
		if("ar".equals(lang))return "ae";
		if("cs".equals(lang))return "cz";
		if("ca".equals(lang))return "es";
		if("da".equals(lang))return "dk";
		if("en".equals(lang))return "gb";
		if("he".equals(lang))return "il";
		if("ja".equals(lang))return "jp";
		if("ko".equals(lang))return "kr";
		if("sl".equals(lang))return "si";
		if("sv".equals(lang))return "se";
		if("uk".equals(lang))return "ua";
		if("vi".equals(lang))return "vn";
		if("zh".equals(lang))return "hk";
		if("zs".equals(lang))return "cn";
		return "";
	}

    @Override
    public String getCountryOfResidence() {
        return super.getCountryOfResidence();
    }

    private void configureCommon(HttpServletRequest request, ServletContext servletContext,
                                 String applicationOverride, boolean affilateCodeFromCookie)
	{
        //retrieve server name
		//can't see how this will work as setIntegrationMethod() is called much later
        if(getIntegrationMethod().equalsIgnoreCase("xml") && request.isSecure()){
            this.setServerName("www.rentalcars.com");
        }else{
		    this.setServerName(com.alltria.hirecars4u.web.common.HireCars4USession.retrieveServerName(request));
        }
		LOG.debug("server name " + getServerName());

		//retrieve server port
		this.setServerPort(com.alltria.hirecars4u.web.common.HireCars4USession.retrieveServerPort(request));
		LOG.debug("server port " + getServerPort());

		//retrieve module
		String module = com.alltria.hirecars4u.web.common.HireCars4USession.retrieveModule(request, servletContext);
		LOG.debug("module " + module);

		//retrieve module application
		String moduleApplication = com.alltria.hirecars4u.web.common.HireCars4USession.retrieveModuleApplication(
				serverName, module);
		if(moduleApplication!=null && !moduleApplication.equals(""))
		{
			this.setApplicationModule(module);
		}
		LOG.debug("module application " + moduleApplication);

		//retrieve language application options
		this.setLanguageApplicationOptions(com.alltria.hirecars4u.web.common.HireCars4USession.retrieveLanguageApplicationOptions(
				this.getServerName(), moduleApplication));

		//retrieve language application
		this.setLanguageApplication(com.alltria.hirecars4u.web.common.HireCars4USession.retrieveLanguageApplication(
				request, this, this.getLanguageApplicationOptions()));

		//retrieve application
		this.setApplication(com.alltria.hirecars4u.web.common.HireCars4USession.retrieveApplication(request,
				this.getServerName(), moduleApplication, this.getLanguageApplication(),
				applicationOverride));
		LOG.debug("application " + getApplication());

		configureMinimal(request, servletContext, applicationOverride, affilateCodeFromCookie, true);

		setDateRangeForDepositAdvert();

		String adCampURL = HireCars4UForm.escapeHtml(request.getParameter("adcamp"));
		if(adCampURL!=null && !adCampURL.equals("")){
			this.setAdCamp(adCampURL);
		}
		String adPlatURL = HireCars4UForm.escapeHtml(request.getParameter("adplat"));
		if(adPlatURL!=null && !adPlatURL.equals("")){
			this.setAdPlat(adPlatURL);
		}
		String adCoURL = HireCars4UForm.escapeHtml(request.getParameter("adco"));
		if(adCoURL!=null && !adCoURL.equals("")){
			this.setAdCo(adCoURL);
		}

		String googleLidURL = HireCars4UForm.escapeHtml(request.getParameter("google_lid"));
		if(googleLidURL!=null && !googleLidURL.equals("")){
			this.setGoogle_lid(googleLidURL);
		}
		String googleCgidURL = HireCars4UForm.escapeHtml(request.getParameter("google_cgid"));
		if(googleCgidURL!=null && !googleCgidURL.equals("")){
			this.setGoogle_cgid(googleCgidURL);
		}

		String connIdURL = HireCars4UForm.escapeHtml(request.getParameter("connectionId"));
		if(connIdURL!=null && !connIdURL.equals("")){
			this.setConnectionId(connIdURL);
		}

		try {
			String Offset=getSystemParameterValue("default", "locale", "timeOffset");
			timeOffset= Integer.parseInt(Offset);
		} catch (Exception e) {
            LOG.warn("Unexpected error parsing time offset", e);
		}
		String promoCode = HireCars4UForm.escapeHtml(request.getParameter("promoCode"));
		this.setPromoCode(promoCode);


		String overrideLocale = HireCars4UForm.escapeHtml(request.getParameter("overrideLocale"));

		//Check if sysparam true
		if(NodeParameters.getInstance().isAffiliateCodeSwitchFixEnabled()) {
			LocaleUtils.setLocaleFromOverride(request,this);
		}
		else{
			if(overrideLocale!=null){
				this.setLocale(new Locale(overrideLocale));
				this.setLocaleString(overrideLocale);
			}
		}

		if(LOG.isDebugEnabled())
			LOG.debug("session locale configured as: locale='" + this.getLocale() + "' language='" + this.getLanguage() + "' sourceMarket='" + this.getSourceMarket() + "' using affiliate='" + this.getAffiliateCode() + "' with a timeOffset of "+timeOffset);

		if(request.getAttribute(HireCars4UAction.ATTR_ORIGINAL_PATH) == null
        		|| request.getAttribute(HireCars4UAction.ATTR_ORIGINAL_PATH).equals("")) {
            request.setAttribute(HireCars4UAction.ATTR_ORIGINAL_PATH, ServletUtils.getPath(request));
        }

		channel = ChannelUtils.getChannel(request);
		traffic = TrafficManager.getInstance().getTraffic(request.getRemoteAddr(), request.getHeader("User-Agent"), channel, this.getAffiliateCode());
		if (traffic == null) {
			traffic = TrafficManager.NORMAL_TRAFFIC;
		}

		SystemParameterManager spm = SystemParameterManager.getInstance();
		String redirectsEnabled = spm.getSystemParameterValue(this.getCountryOfResidence(), this.getApplication(), "default", "default", "default", "redirectsEnabled", "false");
		if ("true".equals(redirectsEnabled)) {
			this.setDevice(DeviceManager.getInstance().getDeviceFromRequest(request));
			this.setDeviceRedirect(RedirectManager.getInstance().getRedirectType(this.getDevice(), this.getChannel()));
		}


		LOG.debug("Configured with "
                + "serverName: " + getServerName() + ", "
                + "application: " + getApplication() + ", ");
	}

	public void configureMinimal(HttpServletRequest request, ServletContext servletContext, boolean setAffiliatePage)
	{
		configureMinimal(request, servletContext, null, true, setAffiliatePage);
	}

	public void configureMinimal(HttpServletRequest request, ServletContext servletContext,
                                 String applicationOverride, boolean affilateCodeFromCookie, boolean setAffiliatePage)
	{
		//retrieve userclass
		this.setUserClass(Session.retrieveUserClass(request));
		LOG.debug("userclass " + getUserClass());

        this.useCommonAffTilesDefs = null;

		CookieData confCookies = getConfigCookieData(request);

        if ( !isCorValid() ) {
            setCountryOfResidence("");
        }



		// Country Of Residence setup
		if (!confCookies.containsKey(COR_LABLE_NAME) && StringUtils.isEmpty(getCountryOfResidence())) {

			String cor = "";
			if(StringUtils.equals(request.getParameter("langCorMap"),"true")){
				if(StringUtils.isNotBlank(request.getParameter("lang"))) {
					cor = getCorFromLang(request.getParameter("lang"));
				}
				else if(StringUtils.isNotBlank(request.getParameter("preflang"))) {
					cor = getCorFromLang(request.getParameter("preflang"));
				}
			}

			if(StringUtils.isNotBlank(cor)){
				this.setCountryOfResidence(cor);
			}

			//Do we check IP address for the current website?
			else if (Boolean.valueOf(getSystemParameterValue("default", "default", "geoipcheck", "false"))) {

				GeoIP geoip = IpUtils.getGeoIp(request, this.ipAddressOverride);

				if(geoip != null && !geoip.isUnknown()) {
					if(Boolean.valueOf(SystemParameterManager.getInstance().getSystemParameterValue(geoip.getIsoCode().toLowerCase(),
																									getApplication(), "default", "default", "default", "useGeoIpResult", "false"))){
						this.setCountryOfResidence(geoip.getIsoCode().toLowerCase());
						this.setCorPopup(Boolean.valueOf(getSystemParameterValue("default", "default", "corpopup", "false")));
					}
				}
				//Country could not be detected (Unknown IP, Anonymous Provider, Satelite Provider)
				else {
					if (geoip == null || geoip.isEU()) {
						this.setCountryOfResidence("a0");
					} else {
						this.setCountryOfResidence(geoip.getIsoCode().toLowerCase());
					}
					this.setCorPopup(Boolean.valueOf(getSystemParameterValue("default", "default", "corpopup", "false")));
					this.setGeoIpFailed(true);
				}

			}
			//Use the default value from system parameter if COR is still not setup
			if(StringUtils.isEmpty(getCountryOfResidence())){
				this.setCountryOfResidence(getSystemParameterValue("default", "locale", "countryOfResidence", "default"));
				this.setCorPopup(Boolean.valueOf(getSystemParameterValue("default", "default", "corpopup", "false")));
			}
		}
		else if (confCookies.containsKey(COR_LABLE_NAME) && StringUtils.isEmpty(getCountryOfResidence())) {
			String cookieValue = confCookies.get(COR_LABLE_NAME);
			this.setCountryOfResidence(cookieValue);
		}
		LOG.debug("country of residence " + getCountryOfResidence());

		//retrieve language
		this.setLanguage(this.getSystemParameterValue("default", "locale", "languageId"));
		LOG.debug("language " + getLanguage());

		//retrieve Source market
		this.setSourceMarket(this.getSystemParameterValue("default", "locale", "sourceMarket"));
		LOG.debug("source market from system parameters " + getSourceMarket());

		if(LOG.isDebugEnabled())
			LOG.debug("session locale set to entry server '" + request.getServerName() + "(" + this.getApplication() + ") from " + request.getRemoteAddr());

		// default the locale based on the application
		this.setLocaleString(this.getSystemParameterValue("default", "locale", "locale", "en"));
		this.setLocale(new Locale(this.getLocaleString()));

		//retrieve Free Text Search Server Name
    this.setFreeTextSearchServerName(this.getSystemParameterValue("default", "FreeTextSearchEngine", "hostname","solr_search_host"));
    this.setFreeTextSearchServerPort(this.getSystemParameterValue("default", "FreeTextSearchEngine", "port", "8080"));

		try {
			this.setLogAllSearches(this.getSystemParameterValue("default", "locale", "logAllSearches").equalsIgnoreCase("true"));
		}
		catch (Exception e){
            LOG.warn("Unexpected error", e);
		}

		try
		{
			VehicleType=this.getSystemParameterValue("default", "default", "VehicleType");
			LOG.debug("Got vehicle type of " + VehicleType);
		}
		catch (Exception e){
            LOG.warn("Unexpected error", e);
		}

		// override the locale, language and sourcemarket if there is an affiliate code on the URL
		String affiliateCode = HireCars4UForm.escapeHtml(request.getParameter("affiliateCode"));

		    if(affiliateCode!=null){
				affilateCodeOnURL=true;
				this.applyAffiliateCode(affiliateCode,request);
			}
			else if(! affilateCodeFromCookie)
			{
				affilateCodeOnURL=false;
				this.clearAffiliateCode();
			}
			else
			{
				try
				{
					String Affcode=null;
					if(CookieUtils.cookieExists(TRACKING_COOKIE_NAME, request))
					{
						CookieData trackingCookie = new CookieData(CookieUtils.getCookieValue(TRACKING_COOKIE_NAME, request, true));
						Affcode = trackingCookie.get(TRACKING_COOKIE_NAME);
					}
					else
					{
						Affcode=getCookieValue(HireCars4UAction.AFFILIATECODE_LABEL,request);
					}
					if (Affcode!=null&&Affcode.trim().length()!=0&&!Affcode.equals(""))
					{
							this.applyAffiliateCode(Affcode,request, setAffiliatePage);
					}
				}
				catch (Exception e){
                    LOG.warn("Unexpected error", e);
				}
			}

	    //Get information from COR
	    ResidenceCountryInfo corInfo = ResidenceCountryInfo.retrieveResidenceCountryInfo(this.getStaticData(), false);
	    if(StringUtils.isNotEmpty(corInfo.getBaseCurrency())){
	    	this.setBaseCurrency(corInfo.getBaseCurrency());
	    }else{
	    	this.setBaseCurrency(this.getSystemParameterValue("default", "default", "currencycode"));
	    }
	    LOG.debug("CoR Info: Base currency = "+baseCurrency);
	    if(corInfo.getDisplayCurrency()!=null){
	    	this.setDisplayCurrency(corInfo.getDisplayCurrency().getCode());
	    	LOG.debug("CoR Info: Display currency = " + corInfo.getDisplayCurrency().getCode());
	    }
	    if(corInfo.getLanguage()!=null){
	    	try{
		    	this.configureLanguage(String.valueOf(corInfo.getLanguage().getId()));
		    	LOG.debug("CoR Info: Language = "+getLanguage());
	    	}catch(HireCars4UException hc4ue){
	    		LOG.error("Error configuring language", hc4ue);
	    	}
	    }
	    if(corInfo.getSourceMarket()!=null && !("").equals(corInfo.getSourceMarket())){
	    	this.setSourceMarket(corInfo.getSourceMarket());
	    	LOG.debug("CoR Info: Source market = " + getSourceMarket());
	    }


        LOG.debug("CoR Info: about to set cor for vanhire3000 = "+getCountryOfResidence());
        if((StringUtils.isBlank(getCountryOfResidence()) || getCountryOfResidence().equalsIgnoreCase("default"))&& getApplication() != null && getApplication().equalsIgnoreCase("www.vanhire3000.com")){
            LOG.debug("CoR Info: setting cor for vanhire3000 = " + getCountryOfResidence());
            setCountryOfResidence("gb");
        }
       LOG.debug("CoR Info: set cor for vanhire3000 = " + getCountryOfResidence());

        if ("true".contentEquals(getSystemParameterValue("default", "default","activateCrossCheckCorIpLogging", "false"))) {

            GeoIP userIpAddress = IpUtils.getGeoIp(request, this.ipAddressOverride);

            if (userIpAddress != null && !userIpAddress.getIsoCode().equalsIgnoreCase(getCountryOfResidence())) {
                ActionEvent actionEvent = new ActionEvent();
                actionEvent.setSessionId(getSessionID());
                actionEvent.setServerName(getServerName());
                actionEvent.setCategory("COR_IP");
                actionEvent.setAction("COR not aligned with IP");
                actionEvent.setLabel(getCountryOfResidence() + " " + userIpAddress.getIsoCode());
                actionEvent.setInfo("");
                actionEvent.setValue(1);
                actionEvent.fire();
            }
        }

	    /*
	     * Used in PreAuthUtil for Pre Auth Even firing
	     */
	    this.setSessionID(request.getSession().getId());

	    this.populateSearchOptions();
	}

    private boolean isCorValid() {
        boolean validateCorsFailsafe =
                  Boolean.parseBoolean(SystemParameterManager.getInstance().getSystemParameterValue(
                          getCountryOfResidence(),
                          "default",
                          "default",
                          "default",
                          "default",
                          "validateCorsFailsafe",
                          "false"
                  ));

        if ( validateCorsFailsafe ) {
            return true;
        }
        else {

            boolean isValid = true;
            if (StringUtils.isNotEmpty(getCountryOfResidence())) {

                ResidenceCountryInfo countryInfo = ResidenceCountryInfo.retrieveResidenceCountryInfo(null,getCountryOfResidence(),null,false);

                if (CollectionUtils.isNotEmpty(countryInfo.getErrors())) {
                    for (Error error : countryInfo.getErrors()) {
                        if ( CODE_OF_COUNTRY_OF_RESIDENCE_INVALID.equals(error.getCode()) ) {
                            isValid = false;
                            break;
                        }
                    }
                }
            }

            return isValid;
        }
    }

    public void reConfigure(HttpServletRequest request, ServletContext servletContext)
	{
		reConfigure(request, servletContext, null);
	}

	public void reConfigure(HttpServletRequest request, ServletContext servletContext,
			String applicationOverride)
	{
		LOG.debug("reconfiguring");

		this.configureCommon(request, servletContext, applicationOverride, true);
	}

	public void configure(HttpServletRequest request, ServletContext servletContext)
	{
		configure(request, servletContext, null);
	}

	public void configure(HttpServletRequest request, ServletContext servletContext,
			String applicationOverride)
	{
        LOG.debug("Configuring Session of type '{}'", getClass().getName());

		//do main configuration
		configureCommon(request, servletContext, applicationOverride, true);


		SessionLifespan sessionLifespan = this.getLifespan();
		sessionLifespan.setSessionId(request.getSession().getId());
		sessionLifespan.setSessionTimeout(request.getSession().getMaxInactiveInterval());
		sessionLifespan.setRemoteIPAddress(request.getRemoteAddr());

		String newLogicIpAddress = IpUtils.getIpAddressFromRequest(request);
		this.getLifespan().setNewRemoteIPAddress(newLogicIpAddress);
		sessionLifespan.setSessionStart();
		sessionLifespan.setEntryServer(this.getServerName());
		sessionLifespan.setReferralPage(request.getHeader("referer"));
		sessionLifespan.setEntryPage(request.getRequestURL() != null ? request.getRequestURL().toString() : "");

        LOG.debug("Configured session lifespan for new session as '{}'", sessionLifespan);

		String PrevIP=getLifespan().getRemoteIPAddress();
		String Forwarded=request.getHeader("HTTP_X_FORWARDED_FOR");

		if(Forwarded!=null){
			IpUtils.findHostnameAsynchronous(Forwarded, this);
		}else{
			IpUtils.findHostnameAsynchronous(PrevIP, this);
		}

		if (Forwarded==null) {
			Forwarded=request.getHeader("X-FORWARDED-FOR");
		}
        //Only record 'forwarded from' remote ip if different from current.
		if (Forwarded!=null && PrevIP != null && !PrevIP.trim().equals(Forwarded.trim())) {
			getLifespan().setRemoteIPAddress(PrevIP+","+Forwarded);
		}
		for(String ip : getLifespan().getRemoteIPAddress().split(",")){
			if(ip.trim().equals("91.151.7.6")){
				localuser = true;
			}
		}

		this.preparePostTrap();

		this.setSet(true);
	}

	/** Creates a new instance of Session */
	public HireCars4USession()    {

	}

	public boolean getLocationsNotComplete()
	{
		return locationsNotComplete;
	}

	public void setLocationsNotComplete(boolean locationsNotComplete)
	{
		if (locationsNotComplete)
		{
			LOG.debug("Flagging as competitor because locations not complete");
			setCompetitor("Locations Blank");
		}
		this.locationsNotComplete=locationsNotComplete;
	}

	public void setCompReason(String ReasonString)
	{
		if(this.ReasonString!=null && !this.ReasonString.equals(""))
		{
			this.ReasonString += ", " + ReasonString;
		}
		else
		{
			this.ReasonString = ReasonString;
		}
	}

	public void addExtraReasonInfo(String extraInfo)
	{
		reasonExtraInfo += "\n\n"+extraInfo;
	}

	public String getCompReason()
	{
		return ReasonString;
	}

	public String getExtraReasonInfo()
	{
		return reasonExtraInfo;
	}

	public void homepageLoaded()    {
		loadedImages=false;
	}

	public void imageLoaded()    {
		loadedImages=true;
	}

	public void potentialCompEmailSent()    {
		CompEmailSent=true;
	}

	public boolean isAffiliate()
	{
		if (getAffiliateCode()==null||getAffiliateCode().trim().length()==0)
		{
			return false;
		}
		return true;
	}

	public String getLanguageIso() {
		return languageIso;
	}

	public void setLanguageIso(String languageIso) {
		this.languageIso = languageIso;
	}

	public String getLanguageIsoUnmapped() {
		return languageIsoUnmapped;
	}

	public void setLanguageIsoUnmapped(String languageIsoUnmapped) {
		this.languageIsoUnmapped = languageIsoUnmapped;
	}

	public boolean hasCompEmailBeenSent()    {
		return CompEmailSent;
	}

	public boolean hasLoadedImages()    {
		if (isAffiliate())
		{
			return true;
		}
		return loadedImages;
	}

	public boolean hasPreTrackedSearches() {
		if (preTrackedSearches.size()==0) {
			return false;
		} else {
			return true;
		}
	}

	public void addPreTrackSearch(String Search) {
		preTrackedSearches.add(Search);
	}

	public ArrayList getPreTrackedSearches() {
		return preTrackedSearches;
	}

	public void clearPreTrackedSearches() {
		preTrackedSearches.clear();
	}

	public void setDepositSearchCompleted(boolean DepositSearchCompleted) {
		this.DepositSearchCompleted=DepositSearchCompleted;
	}

	public boolean getDepositSearchCompleted() {
		return DepositSearchCompleted;
	}

	public void setCompetitor(String reason)
	{
		setCompReason(reason);
		LOG.debug("Set competitor");
		if(!competitor)
		{
			competitor=true;
			this.addExtraReasonInfo("Competitor set here:\n"+
					LoggingUtils.getStackTrace().substring(0,1024)+"...");
		}
	}

	private boolean isThreeScanDiscount(String discountName)
	{
		boolean isthreescancheck=false;
		DiscountManager discountManager = DiscountManager.getInstance();
		Discount discount=discountManager.getDiscount(discountName);
		Iterator it=discount.getRuleLists().iterator();
		while(it.hasNext())
		{
			RuleList ruleList = (RuleList)it.next();
			Iterator rit=ruleList.getRules().iterator();
			while (rit.hasNext())
			{
				Rule rule=(Rule)rit.next();
				if (rule.getName().equals("competitor"))
				{
					isthreescancheck=true;
				}
			}
		}
		return isthreescancheck;
	}

	public void setCompetitorDiscountNameFromCookie(String discountname)
	{
		LOG.debug("Setting competitor discount name from cookie");
		try
		{
			if (discountname!=null && !discountname.equals(""))
			{
				//only set the discount name if it refers to a discount without
				//a rule concerning the isCompetitor (3 scans etc) flag.
				//Competitor cookies should be unaffective for competitor-flag-only discounts
				if (!isThreeScanDiscount(discountname))
				{
					LOG.debug("discount has no competitor rule - valid for setting");
					setCompetitorDiscountName(discountname);
					LOG.debug("Flagging as competitor because of competitor cookie");
					setCompetitor("Competitor Discount Cookie");
				}
				else
				{
					LOG.debug("discount has competitor rule - not valid for setting");
				}
			}
		}
		catch (Exception e)
		{
			LOG.debug("Exception setting competitor discount name from cookie");
		}
	}

	public void setCompetitorDiscountName(String discountname)
	{
		this.competitorDiscountName=discountname;
	}

	public int getSendtolist()
	{
		return sendtolist;
	}

	public int getIncludeinsummary()
	{
		return includeinsummary;
	}

	public void setCompetitorDiscountName(String discountname, int sendtolist, int includeinsummary)
	{
		this.sendtolist=sendtolist;
		this.includeinsummary=includeinsummary;
		this.competitorDiscountName=discountname;
	}

	public String getCompetitorDiscountName()
	{
		return competitorDiscountName;
	}

	public void setCompetitorAffiliateDiscountName(String discountName)
	{
		this.competitorAffiliateDiscountName = discountName;
	}

	public void setCompetitorAffiliateDiscountName(String discountName, int sendtolist, int includeinsummary)
	{
		this.sendtolist=sendtolist;
		this.includeinsummary=includeinsummary;
		this.competitorAffiliateDiscountName = discountName;
	}

	public String getCompetitorAffiliateDiscountName()
	{
		return competitorAffiliateDiscountName;
	}

	public boolean isCompetitor()
	{
		return competitor;
	}

	public boolean isIPCompetitor() {
		return ipCompetitor;
	}

	public void setIsIPCompetitor(boolean ipCompetitor) {
		this.ipCompetitor=ipCompetitor;
		LOG.debug("Flagging as competitor because is ip competitor");
		setCompetitor("IP Competitor");
	}

	public void setPromoCode(String promoCode) {
		this.promoCode=promoCode;
	}

	public String getPromoCode() {
		return promoCode;
	}

	public int getTimeOffset() {
		return timeOffset;
	}

	public void setTimeOffset(int timeOffset) {
		this.timeOffset=timeOffset;
	}



	private void setDateRangeForDepositAdvert() {
		try {
			ContentMessageResources CMR=ContentMessageResourcesFactory.retrieveContentMessageResources("application");
			Locale SpecialOfferLocale=new Locale("depositadvert");
			String DateRange=CMR.getMessage(SpecialOfferLocale,"depositadvert.date.range");
			StringTokenizer Tokens=new StringTokenizer(DateRange,":");
			String Token;
			String StartDate;
			String EndDate;
			DateRange DR=null;
			int Pos;
			while (Tokens.hasMoreTokens()) {
				Token=Tokens.nextToken();
				Pos=Token.indexOf("-");
				if(Pos > -1) {
    				StartDate=Token.substring(0,Pos);
    				EndDate=Token.substring(Pos+1,Token.length());
    				synchronized (SDF) {
    					DR=new DateRange(SDF.parse(StartDate.trim()),SDF.parse(EndDate.trim()));
    				}
    				DateList.add(DR);
				}
			}
		} catch (Exception e) {
            LOG.warn("Error setting date range for deposit advert", e);
		}
	}

	public String getInDateRange(java.util.Date TestDate) {
		for (int i=0;i<DateList.size();i++) {
			if (((DateRange)DateList.get(i)).betweenDates(TestDate)) {
				return "true";
			}
		}
		return "false";
	}

	public void setLogAllSearches(boolean allSearches) {
		this.allSearches=allSearches;
	}

	public boolean getLogAllSearches() {
		return allSearches;
	}

	public String getVehicleType()
	{
		return VehicleType;
	}

	public void setVehicleType(String VehicleType)
	{
		this.VehicleType=VehicleType;
	}

	public SearchResult getYoungDriverFee() {
	    return youngDriverFee;
	}

	public void setYoungDriverFee(SearchResult youngDriverFee) {
	    this.youngDriverFee = youngDriverFee;
	}

	public StaticData getStaticData()
	{
		StaticData data=new StaticData();
		data.setLanguage(this.getLanguage());
		data.setCountryOfResidence(this.getCountryOfResidence());
		data.setAffiliateCode(this.getAffiliateCode());
		data.setVehicleType(VehicleType);
		data.setDisplayCurrency(this.getDisplayCurrency());
		data.setLoyaltyLevel(this.getLoyaltyLevel());
        data.setChannel(this.getChannel());
        data.setCountryOfOrigin(this.getSourceMarket());
        data.setBaseCurrency(this.getBaseCurrency());
		data.setSalesChannel(this.getSalesChannel());
		data.setMobile(RedirectRule.TYPE_MOBILE.equals(this.getDeviceRedirect()));
		data.setPPCInfo(ppcInfo);
        data.setUserClass(this.getUserClass());
        data.setApplication(this.getApplication());
        data.setDevice(this.getDevice());
        data.setPricingExperiments(getPricingExperimentsString());
		return data;
	}

	public void applyAffiliateCodeIfChanged(HttpServletRequest request, String affiliateCode, String originalAffiliateCode){
		if (StringUtils.isNotEmpty(affiliateCode) && !affiliateCode.equals(originalAffiliateCode)) {
			this.clearAffiliateCode();
			applyAffiliateCode(affiliateCode, request, true);

		}
	}

	public void applyAffiliateCode(String affiliateCode, HttpServletRequest request){
		applyAffiliateCode(affiliateCode, request, true);
	}

	public void applyAffiliateCode(String affiliateCode, HttpServletRequest request, boolean setAffiliatePage){
		if(StringUtils.isNotEmpty(affiliateCode)){

			if(NodeParameters.getInstance().isAffiliateCodeSwitchFixEnabled()){

				this.setAffiliateCode(affiliateCode);
				if (StringUtils.isNotEmpty(request.getParameter("affiliateCode"))) {
					this.setSessionAffiliateCode(request.getParameter("affiliateCode"));
				}
				LocaleUtils.setLocaleAll(request,this,affiliateCode);
				this.setSalesChannel(ChannelMappingManager.getInstance().getMapping(affiliateCode).getName());

				Affiliate affiliate = AffiliateManager.getInstance().getAffiliate(affiliateCode);
				if(affiliate!=null){
					setAffiliateExists(true);

					this.setUserClass(affiliateCode);

					//Don't allow them to fall into the trap of not loading images if they are an affiliate.
					imageLoaded();

					if(setAffiliatePage)
					{
						//Check to see if this site is the correct site for the affiliate
						if (request!=null)
						{
							if (affilateCodeOnURL)
							{
								if (StringUtils.isNotEmpty(affiliate.getServerName())
									&& !getApplication().equals(affiliate.getServerName()) && !StringUtils.contains(request.getRequestURI(), "WEB-INF"))
								{
									updateAffiliatePage(affiliate.getServerName(), request);
									OnWrongSite=true;
								}
							}
						}
					}



					this.setAffiliateGroup(affiliate.getAffiliateGroup());
					if(StringUtils.isNotEmpty(affiliate.getHeaderLogo())){
						this.setAffiliateHeaderLogo(affiliate.getHeaderLogo());
					}
					if(StringUtils.isNotEmpty(affiliate.getHeaderLogoHref())){
						this.setAffiliateHeaderLogoHref(affiliate.getHeaderLogoHref());
					}
					this.setProAffiliate(affiliate.isProAffiliate());
					this.setAffShowPhoneNumber(affiliate.isShowPhoneNumber());
					this.setAffAllowCrossSell(affiliate.isAllowCrossSell());
					this.setBlockABTests(affiliate.isBlockABTests());
					this.setShowFullyOnTests(affiliate.isShowFullyOnTests());
					this.setAffShowFts(affiliate.isDisplayFreeTextSearch());
				}else{
					setAffiliateExists(false);
				}

				return;
			}

			// todo once isAffiliateCodeSwitchFixEnabled is a success we will remove the below code

			Affiliate affiliate = AffiliateManager.getInstance().getAffiliate(affiliateCode);
			if(affiliate!=null){
				setAffiliateExists(true);
				//Don't allow them to fall into the trap of not loading images if they are an affiliate.
				imageLoaded();

				if(setAffiliatePage)
				{
					//Check to see if this site is the correct site for the affiliate
					if (request!=null)
					{
						if (affilateCodeOnURL)
						{
							if (StringUtils.isNotEmpty(affiliate.getServerName())
								&& !getApplication().equals(affiliate.getServerName()) && !StringUtils.contains(request.getRequestURI(), "WEB-INF"))
							{
								updateAffiliatePage(affiliate.getServerName(), request);
								OnWrongSite=true;
							}
						}
					}
				}


				this.setUserClass(affiliateCode);
				this.setAffiliateCode(affiliateCode);
				if (StringUtils.isNotEmpty(request.getParameter("affiliateCode"))) {
					this.setSessionAffiliateCode(request.getParameter("affiliateCode"));
				}
				this.setAffiliateGroup(affiliate.getAffiliateGroup());
				if(StringUtils.isNotEmpty(affiliate.getLocale())){
					this.setLocale(new Locale(affiliate.getLocale()));
					this.setLocaleString(affiliate.getLocale());
				}
				if(StringUtils.isNotEmpty(affiliate.getLanguage())){
					this.setLanguage(affiliate.getLanguage());
				}
				if(!affiliate.isProAffiliate() && StringUtils.isNotEmpty(affiliate.getSourceMarket())){
					this.setSourceMarket(affiliate.getSourceMarket());
				}
				if(StringUtils.isNotEmpty(affiliate.getHeaderLogo())){
					this.setAffiliateHeaderLogo(affiliate.getHeaderLogo());
				}
                if(StringUtils.isNotEmpty(affiliate.getHeaderLogoHref())){
                    this.setAffiliateHeaderLogoHref(affiliate.getHeaderLogoHref());
                }
				this.setProAffiliate(affiliate.isProAffiliate());
				this.setAffShowPhoneNumber(affiliate.isShowPhoneNumber());
				this.setAffAllowCrossSell(affiliate.isAllowCrossSell());
				this.setBlockABTests(affiliate.isBlockABTests());
				this.setShowFullyOnTests(affiliate.isShowFullyOnTests());
				this.setAffShowFts(affiliate.isDisplayFreeTextSearch());
			}else{
				setAffiliateExists(false);
			}
		}
	}

    public void setAffiliateHeaderLogoHref(String affiliateHeaderLogoHref) {
        this.affiliateHeaderLogoHref = affiliateHeaderLogoHref;
    }


    public String getAffiliateHeaderLogoHref(){
        return this.affiliateHeaderLogoHref;
    }

    public void clearAffiliateCode(){
			setAffiliateExists(false);

			this.setUserClass("");
			this.setAffiliateCode("");
			this.setAffiliateGroup("");

			if (NodeParameters.getInstance().isAffiliateCodeSwitchFixEnabled()) {
				this.setAffiliateHeaderLogo("");
				this.setAffiliateHeaderLogoHref("");
				this.setProAffiliate(false);
				this.setAffShowPhoneNumber(false);
				this.setAffAllowCrossSell(true);
				this.setBlockABTests(false);
				this.setShowFullyOnTests(true);
				this.setAffShowFts(true);
				this.setUserClass("default");
			}
	}

	public String getShortLocale()
	{
		String LocString=getSystemParameterValue("default", "locale", "locale");
		int Pos=LocString.indexOf('_');

		if (Pos==-1)
		{
			return LocString;
		}
		else
		{
			return LocString.substring(0,Pos);
		}
	}

	public boolean affiliateOnWrongSite()
	{
		return OnWrongSite;
	}

	public String getAffiliatePage()
	{
		return AffiliatePage;
	}

	public void populateSearchOptions(){
		this.searchOptions.clear();
		Collection allOptions = this.filterSystemParameters("vehicleOptions", "*", "show");
		Iterator it = allOptions.iterator();
		while(it.hasNext()){
			SystemParameter sp = (SystemParameter)it.next();
			if("true".equalsIgnoreCase(sp.getValue())){
				SearchOption searchOption = new SearchOption();
				searchOption.setAttribute(sp.getObject());
				Collection optionData = this.filterSystemParameters("vehicleOptions", sp.getObject(), "*");
				Iterator odIt = optionData.iterator();
				while(odIt.hasNext()){
					SystemParameter odSp = (SystemParameter)odIt.next();
					if("heading".equalsIgnoreCase(odSp.getName())){
						searchOption.setHeading(odSp.getValue());
					} else if("show".equalsIgnoreCase(odSp.getName())){
						searchOption.setShow(true);
					} else {
						searchOption.getOptions().add(odSp.getValue());
					}
				}
				this.searchOptions.add(searchOption);
			}
		}
	}

	private void preparePostTrap()
	{
		this.postTrapPositives.clear();
		this.postTrapNegatives.clear();
		for(int i=0; i<4; i++)
		{
			String varName = POST_TRAP_NAMES[i]+"$"+((int)(Math.random()*9999));
			String varVal = String.valueOf((int)(Math.random()*9999));

			if(Math.random()<0.5){
				this.postTrapPositives.add(new String[]{varName, varVal});
			}else{
				this.postTrapNegatives.add(new String[]{varName, varVal});
			}
		}
	}

	private static boolean refererMatchesAPattern(String referer, Collection<String> patterns)
	{
		for(String pattern : patterns)
		{
			if(referer.matches(pattern))
			{
				return true;
			}
		}
		return false;
	}

	//Sets competitor flag if post parameters fail the test
	public void checkPostTrap(HttpServletRequest request)
	{
		boolean postTrapEnabled = Boolean.parseBoolean(SysParamUtils.paramValueDefaultApp(
				this, "postTrap", "default", "enabled", "false"));
		if(postTrapEnabled && !isAffiliate() && request.getParameterMap().size()>0)
		{
			Collection<String> refererPatterns = SysParamUtils.paramMultiValue(this,
					"postTrap", "refererPatterns").values();
			String referer = request.getHeader("Referer");
			LOG.debug("Referer: " + referer);
			if(referer==null || referer.equals("") || refererMatchesAPattern(referer, refererPatterns))
			{
				LOG.debug("Checking post trap");
				boolean triggered = false;
				for(String[] param : postTrapPositives)
				{
					if(request.getParameter(param[0])==null
							|| !request.getParameter(param[0]).equals(param[1]))
					{
						triggered = true;
						break;
					}
				}
				for(String[] param : postTrapNegatives)
				{
					if(request.getParameter(param[0])!=null)
					{
						triggered = true;
						break;
					}
				}
				if(triggered)
				{
					LOG.debug("Post trap triggered");
					this.setCompetitor("Post trap triggered");
				}
			}
		}
	}

	/**
	 * Getter for property searchOptions.
	 * @return Value of property searchOptions.
	 */
	public java.util.ArrayList getSearchOptions() {
		return searchOptions;
	}

	/**
	 * Setter for property searchOptions.
	 * @param searchOptions New value of property searchOptions.
	 */
	public void setSearchOptions(java.util.ArrayList searchOptions) {
		this.searchOptions = searchOptions;
	}

	/**
	 * Getter(s) for property adCamp.
	 * @return Value of property adCamp.
	 */
	public java.lang.String getAdcamp() {
		return getAdCamp();
	}
	public java.lang.String getAdCamp() {
		try{
			if(serverSideCookies.get(HireCars4UAction.ADCAMP_LABEL)!=null){
				return serverSideCookies.get(HireCars4UAction.ADCAMP_LABEL);
			}
		}catch(Exception e){ LOG.warn("Error getting adCamp", e);}

		return "";
	}

	/**
	 * Setter(s) for property adCamp.
	 * @param adCamp New value of property adCamp.
	 */
	public void setAdcamp(java.lang.String adCamp) {
		setAdCamp(adCamp);
	}
	public void setAdCamp(java.lang.String adCamp) {
		// avoid XSS attempt
		String escaped = adCamp.replace("&lt;", "<").replace("&gt;", ">").trim().toLowerCase();
		if (escaped.contains(">") || escaped.contains("<") || escaped.contains("/")) {
			// TODO Possibly log this as a hack attempt
		}
		else {
			serverSideCookies.put(HireCars4UAction.ADCAMP_LABEL, adCamp);
		}
	}

	/**
	 * Getter for property adCo.
	 * @return Value of property adCo.
	 */
	public java.lang.String getAdco() {
		try{
			if(serverSideCookies.get(HireCars4UAction.ADCOUNTRY_LABEL)!=null){
				return serverSideCookies.get(HireCars4UAction.ADCOUNTRY_LABEL);
			}
		}catch(Exception e){ LOG.warn("Error getting adCo", e); }

		return "";
	}

	/**
	 * Setter for property adCo.
	 * @param adCo New value of property adCo.
	 */
	public void setAdco(java.lang.String adCo) {
		serverSideCookies.put(HireCars4UAction.ADCOUNTRY_LABEL, adCo);
	}

	/**
	 * Getter for property adCo.
	 * @return Value of property adCo.
	 */
	public java.lang.String getAdCo() {
		try{
			if(serverSideCookies.get(HireCars4UAction.ADCOUNTRY_LABEL)!=null){
				return serverSideCookies.get(HireCars4UAction.ADCOUNTRY_LABEL);
			}
		}catch(Exception e){ LOG.warn("Error getting adCo", e); }

		return "";
	}

	/**
	 * Setter for property adCo.
	 * @param adCo New value of property adCo.
	 */
	public void setAdCo(java.lang.String adCo) {
		serverSideCookies.put(HireCars4UAction.ADCOUNTRY_LABEL, adCo);
	}

	/**
	 * Getter for property adCountry.
	 * @return Value of property adCountry.
	 */
	public java.lang.String getAdCountry() {
		try{
			if(serverSideCookies.get(HireCars4UAction.ADCOUNTRY_LABEL)!=null){
				return serverSideCookies.get(HireCars4UAction.ADCOUNTRY_LABEL);
			}
		}catch(Exception e){ LOG.warn("Error getting adCountry", e); }

		return "";
	}

	/**
	 * Setter for property adCountry.
	 * @param adCountry New value of property adCountry.
	 */
	public void setAdCountry(java.lang.String adCountry) {
		serverSideCookies.put(HireCars4UAction.ADCOUNTRY_LABEL, adCountry);
	}

	/**
	 * Getter for property adPlat.
	 * @return Value of property adPlat.
	 */
	public java.lang.String getAdPlat() {
		try{
			if(serverSideCookies.get(HireCars4UAction.ADPLAT_LABEL)!=null){
				return serverSideCookies.get(HireCars4UAction.ADPLAT_LABEL);
			}
		}catch(Exception e){ LOG.warn("Error getting adPlat", e); }

		return "";
	}

	/**
	 * Setter for property adPlat.
	 * @param adPlat New value of property adPlat.
	 */
	public void setAdPlat(java.lang.String adPlat) {
		serverSideCookies.put(HireCars4UAction.ADPLAT_LABEL, adPlat);
	}

	/**
	 * Getter for property adPlat.
	 * @return Value of property adPlat.
	 */
	public java.lang.String getAdplat() {
		try{
			if(serverSideCookies.get(HireCars4UAction.ADPLAT_LABEL)!=null){
				return serverSideCookies.get(HireCars4UAction.ADPLAT_LABEL);
			}
		}catch(Exception e){ LOG.warn("Error getting adPlat", e); }

		return "";
	}

	/**
	 * Setter for property adPlat.
	 * @param adPlat New value of property adPlat.
	 */
	public void setAdplat(java.lang.String adPlat) {
		serverSideCookies.put(HireCars4UAction.ADPLAT_LABEL, adPlat);
	}

	public String getEntryPage() {
		return entryPage;
	}

	public void setEntryPage(String entryPage) {
		this.entryPage = entryPage;
	}

	public String getGoogle_lid() {
		return google_lid;
	}

	public void setGoogle_lid(String googleLid) {
		google_lid = googleLid;
	}

	public String getGoogle_cgid() {
		return google_cgid;
	}

	public void setGoogle_cgid(String googleCgid) {
		google_cgid = googleCgid;
	}

	/**
	 * Getter for property sourceMarket.
	 * @return Value of property sourceMarket.
	 */
	public java.lang.String getSourceMarket() {
		return sourceMarket;
	}

	/**
	 * Setter for property sourceMarket.
	 * @param sourceMarket New value of property sourceMarket.
	 */
	public void setSourceMarket(java.lang.String sourceMarket) {
		this.sourceMarket = sourceMarket;
	}

	/**
	 * Getter for property affiliateCode.
	 * @return Value of property affiliateCode.
	 */
	public java.lang.String getAffiliateCode() {
		try{
			if(serverSideCookies.get(HireCars4UAction.AFFILIATECODE_LABEL)!=null){
				return serverSideCookies.get(HireCars4UAction.AFFILIATECODE_LABEL);
			}
		}catch(Exception e){ LOG.warn("Error getting affiliateCode", e); }

		return "";
	}

	/**
	 * Setter for property affiliateCode.
	 * @param affiliateCode New value of property affiliateCode.
	 */
	public void setAffiliateCode(java.lang.String affiliateCode) {
		serverSideCookies.put(HireCars4UAction.AFFILIATECODE_LABEL, affiliateCode);
	}

	public String getAffiliatePassword() {
		return affiliatePassword;
	}

	public void setAffiliatePassword(String val)
	{
		this.affiliatePassword = val;
	}

	public void recordSearch(String location){
		LOG.debug("Recording search");
		if(!competitor&&!localuser){
			if (!countrySearches.contains(location)){
				if (countrySearches.size()<3){
					countrySearches.add(location);
				} else {
					LOG.debug("Flagging as competitor because of 3 country scans");
					setCompetitor("3 Scan Set");
				}
			}
		}
	}

	public String getAffiliateGroup() {
		return affiliateGroup;
	}

	public void setAffiliateGroup(String affiliateGroup) {
		this.affiliateGroup = affiliateGroup;
	}

	private String getCookieValue(String cookieName, HttpServletRequest request)
	{
		String returnValue="";
		Cookie[] cookies = request.getCookies();
		if(cookies!=null){
			for(int i=0;i<cookies.length;i++){
				if(cookieName.equals(cookies[i].getName())) {
					returnValue = cookies[i].getValue();
					break;
				}
			}
		}
		return returnValue;
	}

	public boolean checkAffiliateLogin() {
		if(getAffiliateCode() == null || getAffiliatePassword() == null) {
			return false;
		} else{
			AffiliateManager am = AffiliateManager.getInstance();
			Affiliate affiliate = am.getAffiliate(getAffiliateCode());
			if(affiliate != null && affiliate.authenticate(getAffiliatePassword())) {
				return true;
			} else {
				return false;
			}
		}
	}

	public boolean isProxyIp()
	{
		return getProxyType()!=null;
	}

	public ProxyType getProxyType()
	{
		if(!lookedUpProxy)
		{
			String[] ips = getLifespan().getRemoteIPAddress().split(",");
			for(int i=0; i<ips.length; i++)
			{
				CompetitorProxyLookup lookup = CompetitorProxyLookup.get();
				if (lookup != null) {
					CompetitorProxyInfo proxy = lookup.lookup(ips[i].trim());
					if(proxy != null)
					{
						proxyType = proxy.getProxyType();
					}
					else
					{
						proxyType = null;
					}
				}
			}
			lookedUpProxy = true;
		}
		return proxyType;
	}

	public void configureLanguage(String prefLang) throws HireCars4UException
	{
        this.useCommonAffTilesDefs = null;
		String langId = prefLang;
		if(LocaleUtilsManager.isoExists(prefLang.toLowerCase().trim()))
		{
			langId = LocaleUtilsManager.getLangIdFromIsoCode(prefLang.toLowerCase().trim());
			languageIso = prefLang;
			languageIsoUnmapped = prefLang;
		}
		else
		{
			languageIso = LocaleUtilsManager.getIsoCodeFromLangId(langId);
			try {
				Language languageDetails = LanguageManager.getInstance().getLanguageFromId(langId);
				if (languageDetails != null) {
					languageIsoUnmapped = languageDetails.getLang();
				}
			} catch (Exception e) {
                LOG.warn("Error getting language from langId {}, looking for default in system parameter", langId);
				try {
					SystemParameterManager spm = SystemParameterManager.getInstance();
					langId = spm.getSystemParameterValue(getCountryOfResidence(), "default", "default", this.getUserClass(), "defaultLangId", "defaultLangId", "361");
				} catch (Exception ex) {
					LOG.warn("Error getting defaultLangId system parameter for {}", this.getUserClass());
                    langId = "361";
				}
			}
		}
		this.setLanguage(langId);
		String locale = LocaleUtilsManager.getLocaleFromLangId(this.getApplication(), langId, getAffiliateCode(), getCountryOfResidence());
		if(null != locale)
		{
			this.setLocale(new Locale(locale));
			this.setLocaleString(locale);
		}
		else
		{
			//get the locale
			//get the content message associated with cannot find language
			ContentMessageResources CMR=ContentMessageResourcesFactory.retrieveContentMessageResources("application");
			String errorMessage = CMR.getMessage(this.getLocale(), CANNOT_FIND_LANGUAGE_MESSAGE_KEY);
			String englishErrorMessage = CMR.getMessage(CANNOT_FIND_LANGUAGE_MESSAGE_KEY);

			if(errorMessage.startsWith("???")){
				errorMessage = englishErrorMessage;
			}
			if(! errorMessage.equals(englishErrorMessage)){
				errorMessage += "<br />("+englishErrorMessage+")";
			}
			//throw a custom exception
			throw new HireCars4UException(errorMessage);
		}
                if (isConfiguredForABTests()) {
                    ThreadLocalUtils.setABTests(ABTestFilter.refreshABTests());
                }
	}

	public void configurePlLoyal(HttpServletRequest request) {
		if(Boolean.valueOf(SystemParameterManager.getInstance().getSystemParameterValue(getCountryOfResidence(), getApplication(), "default", "default", "default", "enablePriceLineLoyalty", "false"))) {
			if (RedirectRule.TYPE_MOBILE.equals(deviceRedirect) || device.isTablet() || Arrays.asList(new String[]{"email", "intra-company", "comparison", "affiliates"}).contains(salesChannel)) {
				if (ExperimentManager.VARIANT_B.equals(ExperimentManager.showExperiment("tj_abg_plloyal_2", request, true))) {
					plLoyal = "true";
				}
			}
		}
	}

	public void setProxyType(ProxyType type)
	{
		proxyType = type;
	}

	public boolean isClickthroughCounted() {
		return clickthroughCounted;
	}

	public void setClickthroughCounted(boolean clickthroughCounted) {
		this.clickthroughCounted = clickthroughCounted;
	}

	public boolean isPossibleCompetitor() {
		return isPossibleCompetitor;
	}

	public void setPossibleCompetitor(boolean isPossibleCompetitor)
	{
		this.isPossibleCompetitor = isPossibleCompetitor;
		if(isPossibleCompetitor && !isCaptchaPassed() && !isCaptchaRequired())
		{
			//get system parameter determining if captcha use is enabled
			SystemParameterManager spm = SystemParameterManager.getInstance();
			String paramVal = "false";
			SystemParameter param = spm.getSystemParameter("default",this.getApplication(),
					"search",this.getUserClass(),"possibleCompetitor","useCaptcha");
			if(param!=null)
			{
				paramVal = param.getValue();
			}
			else
			{
				param = spm.getSystemParameter("default", "default", "search", this.getUserClass(),
						"possibleCompetitor", "useCaptcha");
				if(param!=null)
				{
					paramVal = param.getValue();
				}
			}
			LOG.debug("Captcha enabled param - got [" + param + "]");
			if(Boolean.parseBoolean(paramVal))
			{
				setCaptchaRequired(true);
			}
		}
	}

	public void setCaptchaRequired(boolean required){
		LOG.debug("Set captcha required " + required);
		this.captchaRequired = required;
	}

	public boolean isCaptchaRequired() {
		return captchaRequired;
	}

	public void setCaptchaPassed(boolean passed){
		LOG.debug("Set captcha passed " + passed);
		this.captchaPassed = passed;
	}

	public boolean isCaptchaPassed(){
		return captchaPassed;
	}

	public boolean isAffiliateMasterLogin() {
		return affiliateMasterLogin;
	}

	public void setAffiliateMasterLogin(boolean affiliateMasterLogin) {
		this.affiliateMasterLogin = affiliateMasterLogin;
	}

	public String getAffiliateCompanyName() {
		return affiliateCompanyName;
	}

	public void setAffiliateCompanyName(String affiliateCompanyName) {
		this.affiliateCompanyName = affiliateCompanyName;
	}

	public void hostnameResolved(String hostname)
	{
		LOG.debug("Hostname resolved: " + hostname);
		setHostname(hostname);
	}

	public String getHostname() {
		return hostname;
	}

	public void setHostname(String hostname) {
		this.hostname = hostname;
	}

	public boolean getAffiliateExists() {
		return affiliateExists;
	}

	public void setAffiliateExists(boolean affiliateExists) {
		this.affiliateExists = affiliateExists;
	}

	public String getApplicationModule()
	{
		return applicationModule;
	}
	public void setApplicationModule(String applicationModule)
	{
		this.applicationModule = applicationModule;
	}

	public String getServerPort()
	{
		return serverPort;
	}

	public void setServerPort(String serverPort)
	{
		this.serverPort = serverPort;
	}

	public String getServerName()
	{
		return serverName;
	}

	public void setServerName(String serverName)
	{
		this.serverName = serverName;
		//LOG.error("server name changed", new Exception(serverName));
	}

	public String getFreeTextSearchServerName() {
		return freeTextSearchServerName;
	}

	public void setFreeTextSearchServerName(String freeTextSearchServerName) {
		this.freeTextSearchServerName = freeTextSearchServerName;
	}

	public String getFreeTextSearchServerPort() {
		return freeTextSearchServerPort;
	}

	public void setFreeTextSearchServerPort(String freeTextSearchServerPort) {
		this.freeTextSearchServerPort = freeTextSearchServerPort;
	}

	public void setConnectionId(String connectionId) {
		this.connectionId = connectionId;
	}

	public String getConnectionId() {
		return connectionId;
	}

	public String getAltLang() {
		return altLang;
	}

	public void setAltLang(String altLang) {
		this.altLang = altLang;
	}

	public String getLanguageApplication()
	{
		return languageApplication;
	}

	public void setLanguageApplication(String languageApplication)
	{
		this.languageApplication = languageApplication;
	}

	@Override
	public void setLanguage(String language) {
		String languageIsoCode = LocaleUtilsManager.getIsoCodeFromLangId(language);
		if(null != languageIsoCode){
			setLanguageIso(languageIsoCode);
		}
		super.setLanguage(language);

		try {
			if(Switcher.INSTANCE.execute(Switcher.HC4U_SESSION_LOGGING)) {
				if (!NumberUtils.isNumber(language)) {
					final String error = "Language " + language + " should be passed in a number format.";
					final String message = language + " should be passed in a number format rather than country iso format.";
					final String type = "invalidLanguage";

					Exception langException = new Exception(message);

					HttpServletRequest request = RequestThreadLocalFilter.getRequest();

					EventLoggerUtils.getInstance().fireErrorEvent(request, langException, error, message, type);
				}
			}
		} catch (Exception e) {
			LOG.warn("Error logging langauge not a number for {}", language);
		}
	}

	public void setLanguageApplicationOptions(HashSet<String> options)
	{
		this.languageAppOptions = options;
	}

	public Set<String> getLanguageApplicationOptions()
	{
		return languageAppOptions;
	}

	public boolean isDenyAccess()
	{
		return denyAccess;
	}

	public void setDenyAccess(boolean denyAccess)
	{
		this.denyAccess = denyAccess;
	}

	public List<String[]> getPostTrapPositives()
	{
		return postTrapPositives;
	}

	public void setPostTrapPositives(ArrayList<String[]> postTrapPositives)
	{
		this.postTrapPositives = postTrapPositives;
	}

	public List<String[]> getPostTrapNegatives()
	{
		return postTrapNegatives;
	}

	public void setPostTrapNegatives(ArrayList<String[]> postTrapNegatives)
	{
		this.postTrapNegatives = postTrapNegatives;
	}

	public OnSaleType getOnSaleType()
	{
		return OnSaleType.valueOf(this.getSystemParameterValue("default", "default",
				"onSaleType", OnSaleType.WEBSITE.name()));
	}

	public int getPreAuthRetries()
	{
		return preAuthRetries;
	}

	public void setPreAuthRetries(int preAuthRetries)
	{
		this.preAuthRetries = preAuthRetries;
	}

	public boolean isAuthSuccess() {
		return authSuccess;
	}

	public void setAuthSuccess(boolean authSuccess) {
		this.authSuccess = authSuccess;
	}

	public boolean isPayLocal() {
		return payLocal;
	}

	public void setPayLocal(boolean payLocal) {
		this.payLocal = payLocal;
	}

	public String getIntegrationMethod() {
		return integrationMethod;
	}

	public void setIntegrationMethod(String integrationMethod) {
		this.integrationMethod = integrationMethod;
	}

	public Traffic getTraffic() {
		return traffic;
	}

	public TrafficClassification getTrafficClassification() {
		return trafficClassification;
	}


	public void putServerSideCookie(String cookieName, String cookieValue)
	{
		serverSideCookies.put(cookieName, cookieValue);
	}

	public String getServerSideCookieValues(String cookieName)
	{
		return serverSideCookies.get(cookieName);
	}

	public ExperimentCookie getExperimentCookie() {
		return experimentCookie;
	}

	public void setExperimentCookie(ExperimentCookie experimentCookie) {
                //diagnostic code
                if(experimentCookie == null) {
                    LOG.error("experimentCookie should never be set to null", new NullPointerException());
                    return;
                }
                this.experimentCookie = experimentCookie;
	}

	public String getClickTaleCode() {
		return clickTaleCode;
	}

	public void setClickTaleCode(String clickTaleCode) {
		this.clickTaleCode = clickTaleCode;
	}

	public String getSalesChannel() {
		return salesChannel;
	}

	public void setSalesChannel(String salesChannel) {
		this.salesChannel = salesChannel;
	}

	public String getQuoteRef() {
		return quoteRef;
	}

	public void setQuoteRef(String quoteRef) {
		this.quoteRef = quoteRef;
	}

    public void setAffiliateMasterSearch(boolean affiliateMasterSearch)
    {
        this.affiliateMasterSearch = affiliateMasterSearch;
    }


    public boolean getAffiliateMasterSearch()
    {
        return affiliateMasterSearch;
    }

    public boolean isAffiliateMasterSearch()
    {
        return affiliateMasterSearch;
    }
    public void setAffiliateLogin(String affiliateLogin)
    {
        this.affiliateLogin = affiliateLogin;
    }

    public String getAffiliateLogin()
    {
        return affiliateLogin;
    }

	public String getWhiteLabelServer() {
		// returns default serverName if parameter doesnt exist
		SystemParameterManager spm = SystemParameterManager.getInstance();
		String affiliateCode = this.getAffiliateCode();
		Affiliate affiliate = AffiliateManager.getInstance().getAffiliate(this.sessionAffiliateCode);

		boolean wlFixActive = Boolean.valueOf(spm.getSystemParameterValue("default", serverName, "default", affiliateCode, "default",
				"whiteLabelServerFixActive", "false"));

		if ("".equals(affiliateCode) || affiliateCode == null) {
			// whitelabel sites should never be active without an affiliatecode.
			return this.serverName;
		} else {
			if(wlFixActive) {
				String wlServer = spm.getSystemParameterValue("default", serverName, "default", affiliateCode, "locale",
						"whiteLabelServer", this.serverName);
				// hack to get opodo's stupid language specific sub domains working
				boolean wlServerLangBased = Boolean.valueOf(spm.getSystemParameterValue("default", serverName, "default", affiliateCode, "default",
						"whiteLabelServerLangBased", "false"));
				if (wlServerLangBased) {
					// This allows for language change on another domain eg French
					// translations on booking.easycar.de
					// We're using the original language defined for easycar, as we
					// want to keep the customer in the same domain and it's defined
					// based on that
					String domainSpecificWLServer = null;
					String preflang = RequestThreadLocalFilter.getParameter("preflang");
					if (!StringUtils.isEmpty(preflang)) {
						String langId = "";
						if (StringUtils.isNumeric(preflang)) {
							langId = preflang;
						} else {
							langId = LocaleUtilsManager.getLangIdFromIsoCode(preflang);
						}
						domainSpecificWLServer = spm.getSystemParameterValue("default", serverName, "default",
								affiliateCode,
								langId,
								"whiteLabelServer", wlServer);
					} else {
						domainSpecificWLServer = spm.getSystemParameterValue("default", serverName, "default",
								affiliateCode, this.getLanguage(), "whiteLabelServer", wlServer);
					}

					if (StringUtils.isNotEmpty(domainSpecificWLServer)) {
						wlServer = domainSpecificWLServer;
					}

					if (affiliate != null) {
						affiliate.setServerName(wlServer);
					}
				}

				return wlServer;
			}
			else {
				String wlServer = spm.getSystemParameterValue("default",serverName,"default",
						affiliateCode, "locale", "whiteLabelServer", this.serverName);
				//hack to get opodo's stupid language specific sub domains working
				if(affiliateCode.equals("opodo") || affiliateCode.equals("opodo_de") || affiliateCode.equals("bravofly") || affiliateCode.equals("easycar_rc")) {
					wlServer = spm.getSystemParameterValue("default",serverName,"default",
							affiliateCode, this.getLanguage(), "whiteLabelServer", wlServer);
					if(affiliate != null) {
						affiliate.setServerName(wlServer);
					}

					// If easycar, we need to use the current domain rather than the system parameter domain
					// This allows for language change on another domain eg French translations on booking.easycar.de
					if (affiliateCode.equals("easycar_rc") && this.RequestURL != "") {

		            	URI affiliateUrl = null;
	    				try {
							affiliateUrl = new URI(this.RequestURL);
						} catch (URISyntaxException e) {
							// If this fails, the wlServer is defined by the system parameter as above
							e.printStackTrace();
						}

		            	if (affiliateUrl.getHost() != null && !affiliateUrl.getHost().contains(wlServer)) {
		            		wlServer = affiliateUrl.getHost();
		                	affiliate.setServerName(wlServer);
		            	}
		            }

	            } else if (affiliateCode.equals("getcar")) {
	            	wlServer = "getcar.net";
	            	affiliate.setServerName(wlServer);
	            }

				return wlServer;
			}
		}
	}

	public boolean isRateCodeExpired() {
		return rateCodeExpired;
	}

	public boolean isRealVisit() {
		Traffic traffic = getTraffic();
		if(traffic != null) {
			String classification = traffic.getClassification();
			return "N".equals(classification);
		}
		return false;
	}

	public void setRateCodeExpired(boolean rateCodeExpired) {
		this.rateCodeExpired = rateCodeExpired;
	}

	public String getRateCodeExpirationReason() {
		return rateCodeExpirationReason;
	}

	public void setRateCodeExpirationReason(String rateCodeExpirationReason) {
		this.rateCodeExpirationReason = rateCodeExpirationReason;
	}

	public String getRateCodeExpirationClass() {
		return rateCodeExpirationClass;
	}

	public void setRateCodeExpirationClass(String rateCodeExpirationClass) {
		this.rateCodeExpirationClass = rateCodeExpirationClass;
	}

	public boolean isOptimiseHtml() {
		return optimiseHtml;
	}

	public void setOptimiseHtml(boolean optimiseHtml) {
		this.optimiseHtml = optimiseHtml;
	}

	public boolean isBadPostalCode() {
		return badPostalCode;
	}

	public void setBadPostalCode(boolean badPostalCode) {
		this.badPostalCode = badPostalCode;
	}

	public boolean isReferrerOurSite() {
		return isReferrerOurSite;
	}

	public void setReferrerOurSite(boolean isReferrerOurSite) {
		this.isReferrerOurSite = isReferrerOurSite;
	}

	public String getRefId() {
		return refId;
	}

	public void setRefId(String refId) {
		this.refId = refId;
	}

	public String getRefClickId() {
		return refClickId;
	}

	public void setRefClickId(String refClickId) {
		this.refClickId = refClickId;
	}

	public boolean isCookiePopupViewed() {
		boolean returnValue =  cookiePopupViewed;

		if(!returnValue){
			cookiePopupViewed = true;
		}
		return returnValue;
	}

	public void setCookiePopupViewed(boolean cookiePopupViewed) {
		this.cookiePopupViewed = cookiePopupViewed;
	}

	public String getMd5() {
		String creativeIdMd5Value = getLabel();
		String md5 = "";
		int md5Start;
		if(creativeIdMd5Value == null) {
			LOG.debug("get creative md5 from server side cookies returned null");
		} else if(creativeIdMd5Value.indexOf("-") != -1) {
			String isBingAffiliate = SystemParameterManager.INSTANCE.getSystemParameterValue("default", "default", "default", getAffiliateCode(), "default", "BingAffliliatePPCTrack");
			if(isBingAffiliate.equals("true")) {
				md5Start = creativeIdMd5Value.lastIndexOf("-") - 22;
			} else {
				md5Start = creativeIdMd5Value.lastIndexOf("-") + 1;
		}
		int md5End = md5Start + 22;
			if(md5End <= creativeIdMd5Value.length() && md5Start >=0 && md5Start < creativeIdMd5Value.length()) {
		    return creativeIdMd5Value.substring(md5Start, md5End);
		}
	}
		return md5;
	}

	public String getCreativeId() {
		String creativeIdMd5Value = getLabel();
		String creativeId = "";
        if(creativeIdMd5Value == null) {
			LOG.debug("get creative id from server side cookies returned null");
		} else if(creativeIdMd5Value.indexOf("-") != -1) {
			String isBingAffiliate = SystemParameterManager.INSTANCE.getSystemParameterValue("default", "default", "default", getAffiliateCode(), "default", "BingAffliliatePPCTrack");
			if(isBingAffiliate.equals("true")) {
				int creativeIdStart = creativeIdMd5Value.lastIndexOf("-") + 1;
				if(creativeIdStart < creativeIdMd5Value.length()) {
					creativeId = "S" + creativeIdMd5Value.substring(creativeIdStart);
        }
			} else {
        int creativeIdStart = creativeIdMd5Value.lastIndexOf("-") + 23;
				if(creativeIdStart < creativeIdMd5Value.length()) {
					creativeId = creativeIdMd5Value.substring(creativeIdStart);
				}
			}
        }
		return creativeId;
	}

	public String getLabel() {
		try{
			return serverSideCookies.get(HireCars4UAction.CREATIVEID_MD5_LABEL);
		}catch(Exception e){ LOG.warn("Error getting label", e); }

		return "";
	}

	public void setLabel(String label) {
		serverSideCookies.put(HireCars4UAction.CREATIVEID_MD5_LABEL, label);
	}

	public boolean isCorPopup() {
		return corPopup;
	}

	public void setCorPopup(boolean corPopup) {
		this.corPopup = corPopup;
	}

	public String getDisplayCurrency() {
		return displayCurrency;
	}

	public void setDisplayCurrency(String displayCurrency) {
		this.displayCurrency = displayCurrency;
	}

	public String getBaseCurrency() {
		return baseCurrency;
	}

	public void setBaseCurrency(String baseCurrency) {
		this.baseCurrency = baseCurrency;
	}

	public boolean isGeoIpFailed() {
		return geoIpFailed;
	}

	public void setGeoIpFailed(boolean geoIpFailed) {
		this.geoIpFailed = geoIpFailed;
	}

	public CustomerDataCookie getCustomerDataCookie() {
		return customerDataCookie;
	}

	public void setCustomerDataCookie(CustomerDataCookie customerDataCookie) {
		this.customerDataCookie = customerDataCookie;
	}

	public static CookieData getTrackingCookieData(HttpServletRequest request){
		CookieData trackCookies = null;
		if(CookieUtils.cookieExists(TRACKING_COOKIE_NAME,request)){
			trackCookies = new CookieData(CookieUtils.getCookieValue(TRACKING_COOKIE_NAME, request, true));
		}
		else
		{
			trackCookies = new CookieData();
			for(String cookieName: TRACKING_DATA_LABELS)
			{
				if(CookieUtils.cookieExists(cookieName, request)){
					trackCookies.put(cookieName, CookieUtils.getCookieValue(cookieName, request));
				}
			}
		}
		return trackCookies;
	}

    public CookieData getConfigCookieData(HttpServletRequest request){
    	CookieData confCookies = null;
		if(CookieUtils.cookieExists(CONF_COOKIE_NAME,request)){
			confCookies = new CookieData(CookieUtils.getCookieValue(CONF_COOKIE_NAME, request));
		}
		else
		{
			confCookies = new CookieData();
			for(String cookieName: CONFIGURATION_DATA_LABELS)
			{
				if(CookieUtils.cookieExists(cookieName, request)){
					confCookies.put(cookieName, CookieUtils.getCookieValue(cookieName, request));
				}
			}
		}
		return confCookies;
	}

    public static boolean existsConfCookie(HttpServletRequest request){
		if(CookieUtils.cookieExists(CONF_COOKIE_NAME,request)){
			return true;
		}
		return false;
    }

    public static void updateTjConfCookie(com.alltria.hirecars4u.web.common.HireCars4USession hc4uSession, HttpServletRequest request, HttpServletResponse response) {

        CookieData cookieData = new CookieData();

        if(Boolean.valueOf(hc4uSession.getSystemParameterValue("default", "default", "corcookie", "false"))) {
			cookieData.put(com.alltria.hirecars4u.web.common.HireCars4USession.COR_LABLE_NAME, hc4uSession.getCountryOfResidence());
        }

        if(Boolean.valueOf(hc4uSession.getSystemParameterValue("default", "default", "changeLanguage", "false"))) {
            if(StringUtils.isNotEmpty(hc4uSession.getLanguageIso())) {
                cookieData.put(LocaleUtilsManager.LANGUAGE_LABEL, hc4uSession.getLanguageIso());
            } else {
                cookieData.put(LocaleUtilsManager.LANGUAGE_LABEL, hc4uSession.getLanguage());
            }
        }

        if(StringUtils.isNotEmpty(hc4uSession.getDisplayCurrency()) && Boolean.valueOf(hc4uSession.getSystemParameterValue("default", "default", "changeCurrency", "false"))) {
            cookieData.put(com.alltria.hirecars4u.web.common.HireCars4USession.CURRENCY_LABLE_NAME, hc4uSession.getDisplayCurrency());
        }

        if(cookieData.size() > 0) {
            response.addCookie(CookieUtils.createCookie(com.alltria.hirecars4u.web.common.HireCars4USession.CONF_COOKIE_NAME, cookieData.toString(), request));
        }
    }

    public PricingExperimentCookie getPricingExperiments() {
		return pricingExperiments;
	}

	public void setPricingExperiments(PricingExperimentCookie pricingExperiments) {
		this.pricingExperiments = pricingExperiments;
	}

	public String getPricingExperimentsString() {
		if(isPricingExperimentsActive()) {
			return pricingExperimentsString;
		} else {
			return "";
		}
	}

	public void setPricingExperimentsString(String pricingExperimentsString) {
		if(this.pricingExperimentsString == null) {
			//we don't want this to change after it is first set
			this.pricingExperimentsString = pricingExperimentsString;
		}
	}

	public boolean isLogged() {
		return logged;
	}

	public void setLogged(boolean logged) {
		this.logged = logged;
	}

	public void rollbackBookingSettings() {
    	sourceMarket = backupSourceMarket;
    	backupSourceMarket = "";
    	baseCurrency = backupBaseCurrency;
		backupBaseCurrency = "";
	}

    public void configureToBookingSettings(String bookingSourceMarket, String bookingBaseCurrency) {
		backupSourceMarket = sourceMarket;
		sourceMarket = bookingSourceMarket;
		backupBaseCurrency = baseCurrency;
		baseCurrency = bookingBaseCurrency;
	}

    /**
     * Checks whether the set up booking source market is equal to session source market.
     * @param bookingSourceMarket - booking source market property
     * @param bookingBaseCurrency - booking basic pricing currency
     * @return {@code true} if the booking source market is set and not equal to the ones set in the session
     * e.g. needs to be configured to the booking settings,
     * otherwise {@code false}
     * @author loginam
     */
    public boolean needsToBeConfiguredToBookingSettings(String bookingSourceMarket, String bookingBaseCurrency){
        return (StringUtils.isNotEmpty(bookingSourceMarket) && StringUtils.isNotEmpty(bookingBaseCurrency)
                && !bookingSourceMarket.equals(getSourceMarket()));
    }

	public boolean isUsingBookingSettings(){
		return !backupBaseCurrency.equals("") && !backupSourceMarket.equals("");
	}

	public String getIpAddressOverride() {
		return ipAddressOverride;
	}

	public void setIpAddressOverride(String ipAddressOverride) {
		this.ipAddressOverride = ipAddressOverride;
	}

	public boolean isIgnorePricingExperiments() {
		return ignorePricingExperiments;
	}

	/**
	 * The request parameters to any action can optionally include
	 * ignorePricingExperiments=true.
	 *
	 * @return false if ignorePricingExperiments=true
	 */

	public boolean isPricingExperimentsActive() {
		return !ignorePricingExperiments;
	}

	public void setIgnorePricingExperiments(boolean ignorePricingExperiments) {
		this.ignorePricingExperiments = ignorePricingExperiments;
	}

	public String getAffiliateHeaderLogo() {
		return affiliateHeaderLogo;
	}

	public void setAffiliateHeaderLogo(String affiliateHeaderLogo) {
		this.affiliateHeaderLogo = affiliateHeaderLogo;
	}

	public boolean isProAffiliate() {
		return proAffiliate;
	}

	public void setProAffiliate(boolean proAffiliate) {
		this.proAffiliate = proAffiliate;
	}

	public boolean isAffShowPhoneNumber() {
		return affShowPhoneNumber;
	}

	public void setAffShowPhoneNumber(boolean affShowPhoneNumber) {
		this.affShowPhoneNumber = affShowPhoneNumber;
	}

	public boolean isAffAllowCrossSell() {
		return affAllowCrossSell;
	}

	public void setAffAllowCrossSell(boolean affAllowCrossSell) {
		this.affAllowCrossSell = affAllowCrossSell;
	}

	public boolean isBlockABTests() {
		return blockABTests;
	}

	public void setBlockABTests(boolean blockABTests) {
		this.blockABTests = blockABTests;
	}

	public boolean isShowFullyOnTests() {
		return showFullyOnTests;
	}

	public void setShowFullyOnTests(boolean showFullyOnTests) {
		this.showFullyOnTests = showFullyOnTests;
	}

	public boolean isAffShowFts() {
		return affShowFts;
	}

	public void setAffShowFts(boolean affShowFts) {
		this.affShowFts = affShowFts;
	}

	public StoredSearchIFace getStoredSearch() {
		return storedSearch;
	}

	public void setStoredSearch(StoredSearchIFace storedSearch) {
		this.storedSearch = storedSearch;
	}

	public String getSessionAffiliateCode() {
		return sessionAffiliateCode;
	}

	public void setSessionAffiliateCode(String sessionAffiliateCode) {
		this.sessionAffiliateCode = sessionAffiliateCode;
	}

	public MailClickData getMailClickData() {
		return mailClickData;
	}

	public void setMailClickData(MailClickData mailClickData) {
		this.mailClickData = mailClickData;
	}

	public String getTduid() {
		return tduid;
	}

	public void setTduid(String tduid) {
		this.tduid = tduid;
	}

	public String getPricingEngineRateCodeOverride() {
		return pricingEngineRateCodeOverride;
	}

	public void setPricingEngineRateCodeOverride(
			String pricingEngineRateCodeOverride) {
		this.pricingEngineRateCodeOverride = pricingEngineRateCodeOverride;
	}

	public String getPlLoyal() {
		return plLoyal;
	}
	public void setPlLoyal(String plLoyal) {
		this.plLoyal = plLoyal;
	}

	public String getPaymentToken() {
		return paymentToken;
	}

	public void setPaymentToken(String paymentToken) {
		this.paymentToken = paymentToken;
	}

	public PaymentInfo getTokenCreditCard() {
		return tokenCreditCard;
	}

	public void setTokenCreditCard(PaymentInfo tokenCreditCard) {
		this.tokenCreditCard = tokenCreditCard;
	}

    public void updateAffiliatePage(String serverName, HttpServletRequest request){
        AffiliatePage=serverName + request.getRequestURI()+"?"+request.getQueryString();
    }

    /***
     * Checks for ignore limit between prices in case of Amend Booking
     * @param bookingTotal = new booking total
     * @return true or false
     */
    public boolean checkAmendIgnoreLimit(double bookingTotal)
    {
    	try
    	{
    		double limit = 1;
    		if(SystemParameterManager.getInstance().getSystemParameterValue(this.getCountryOfResidence(), "default", "default", "default", "IgnoreAmountLimit", "ignoreAmountLimit", "0")!=null)
    		{
    			limit = Double.parseDouble(SystemParameterManager.getInstance().getSystemParameterValue(this.getCountryOfResidence(), "default", "default", "default", "IgnoreAmountLimit", "ignoreAmountLimit", "1"));
    		}

    		if(Math.abs(bookingTotal - this.amendBookingAmount) <= limit)
    			return true;
    		else
    			return false;
    	}
    	catch(Exception e)
    	{
            LOG.warn("Error getting ignoreAmountLimit", e);
    		return false;
    	}
    }

	public Login getLogin() {
		if(login == null) {
			this.login = new Login();
		}
		return login;
	}

	public Login getLogin(HttpServletRequest request)
	{
		if(login == null || login.getIdentity() == null) {
			try
			{
				Login cookieLogin = CookieUtils.getCookieObject(CRM_COOKIE_NAME, request, Login.class);
				if(cookieLogin != null && !ThreadLocalUtils.isBVariant("tj_disable_soft_login_cookie"))
				{
					return cookieLogin;
				}
			}catch(Exception e)
			{
				//TODO: handle exception in the proper way
				e.printStackTrace();
			}

			return new Login();
		}
		return login;
	}

	public void setLogin(Login login) {
		this.login = login;
	}

	public boolean isPreviouslyLoggedIn() {
		return previouslyLoggedIn;
	}

	public void setPreviouslyLoggedIn(boolean previouslyLoggedIn) {
		this.previouslyLoggedIn = previouslyLoggedIn;
	}

	public boolean isPreviouslyLoggedInLoyal() {
        return previouslyLoggedInLoyal;
    }

    public void setPreviouslyLoggedInLoyal(boolean previouslyLoggedInLoyal) {
        this.previouslyLoggedInLoyal = previouslyLoggedInLoyal;
    }

    public boolean getHasSeenNoResults() {
    	return hasSeenNoResults;
    }

    public void setHasSeenNoResults(boolean hasSeenNoResults) {
    	this.hasSeenNoResults = hasSeenNoResults;
    }

	public String getCRMUserId()
	{
		if(getLogin() != null && getLogin().getIdentity() != null)
		{
			return getLogin().getIdentity().getId();
		}
		return "";
	}

	public boolean isSkipLangCurrCheck() {
		return skipLangCurrCheck;
	}

	public void setSkipLangCurrCheck(boolean skipLangCurrCheck) {
		this.skipLangCurrCheck = skipLangCurrCheck;
	}

	public Integer getOldLanguage() {
		return oldLanguage;
	}

	public void setOldLanguage(Integer oldLanguage) {
		this.oldLanguage = oldLanguage;
	}

	public OneWayFee getOneWayFee() {
	    return oneWayFee;
	}

	public void setOneWayFee(OneWayFee oneWayFee) {
	    this.oneWayFee = oneWayFee;
	}

	public String getSessionID() {
		return sessionID;
	}

	public void setSessionID(String sessionID) {
		this.sessionID = sessionID;
	}

    public String getPromotionEnabler() {
        return promotionEnabler;
    }

    public void setPromotionEnabler(String promotionEnabler) {
        this.promotionEnabler = promotionEnabler;
    }

    //Although there is a 1:1 relationship between session threads and session objects
    //there is no stopping from accessing this object from another worker thread.
	private volatile transient ReadableUserAgent storedReadableUserAgent = null;
    public ReadableUserAgent getReadableUserAgent()
    {
        ReadableUserAgent storedReadableUserAgentTmp = storedReadableUserAgent;
    	if (storedReadableUserAgentTmp != null) {
			return storedReadableUserAgentTmp;
        }
        else {
            CacheApi cache = CacheApi.cacheApi("readableuseragent");
            String key = CacheApi.createCacheKey("getReadableUserAgent", userAgentString);
            return cache.get(key, new Cacheable<ReadableUserAgent>() {
                @Override
                public ReadableUserAgent refresh() {
                    return getReadableUserAgentNoCache();
                }
            });
        }
    }

	private ReadableUserAgent getReadableUserAgentNoCache()
	{
		ReadableUserAgent readableUserAgent = null;
		if(userAgentString != null && StringUtils.isNotEmpty(userAgentString)) {
			UserAgentStringParser parser = UADetectorServiceFactory.getResourceModuleParser();
			readableUserAgent = parser.parse(userAgentString);
		}
		storedReadableUserAgent = readableUserAgent;
		return readableUserAgent;
	}

	public void setReadableUserAgent(String userAgent)
	{
		storedReadableUserAgent = null;
		userAgentString = userAgent;
	}
	public String getCustomerType()
	{
		return customerType;
	}
	public void setCustomerType(String customerType)
	{
		this.customerType = customerType;
	}

	public List<CustomerPromotion> getCustomerPromotions() {
		return customerPromotions;
	}

	public void setCustomerPromotions(List<CustomerPromotion> customerPromotions) {
		if(customerPromotions != null) {
			this.customerPromotions = new ArrayList<CustomerPromotion>(customerPromotions);
		} else {
			this.customerPromotions = null;
		}
	}

	public int getLoyaltyLevel() {
        return loyaltyLevel;
    }

    public void setLoyaltyLevel(int loyaltyLevel) {
        this.loyaltyLevel = loyaltyLevel;
    }
    public String getEntryUrl()
	{
		return entryUrl;
	}
	public void setEntryUrl(String entryUrl)
	{
		this.entryUrl = entryUrl;
	}
	public String getEntryQueryString()
	{
		return entryQueryString;
	}
	public void setEntryQueryString(String entryQueryString)
	{
		this.entryQueryString = entryQueryString;
	}

    public void setBooking(Booking booking) {
        this.booking = booking;
    }

    public Booking getBooking() {
        return booking;
    }

    public void setNowChangedLanguage(boolean nowChangedLanguage) {
    	this.nowChangedLanguage = nowChangedLanguage;
    }

    public boolean getNowChangedLanguage() {
    	return nowChangedLanguage;
    }

    /**
     * Set the previous SEO Category language-instance URL slug that the user was accessing.
     *
     * @param seoCategoryPrevSlug
     */
    public void setSeoCategoryPrevSlug(String seoCategoryPrevSlug) {
    	this.seoCategoryPrevSlug = seoCategoryPrevSlug;
    }

    /**
     * Get the previous SEO Category language-instance URL slug that the user was accessing.
     *
     * @return
     */
    public String getSeoCategoryPrevSlug() {
    	return seoCategoryPrevSlug;
    }

    /**
     * Set the previous SEO Post language-instance URL slug that the user was accessing.
     *
     * @param seoPostPrevSlug
     */
    public void setSeoPostPrevSlug(String seoPostPrevSlug) {
    	this.seoPostPrevSlug = seoPostPrevSlug;
    }

    /**
     * Get the previous SEO Post language-instance URL slug that the user was accessing.
     *
     * @return
     */
    public String getSeoPostPrevSlug() {
    	return seoPostPrevSlug;
    }

    public String getPickupLocation() {
        return pickupLocation;
    }


    public void setPickupLocation(Location pickupLocation) {
        this.pickupLocation = getLocationString(pickupLocation);
    }

    public String getDropoffLocation() {
        return dropoffLocation;
    }

    public void setDropoffLocation(Location dropoffLocation) {
        this.dropoffLocation = getLocationString(dropoffLocation);
    }

    private static String getLocationString(Location location) {
        String result = null;

        result = location.getId();

        return result;
    }

    private int abTestsVersion = 0;

    public int getABTestsVersion() {
        return abTestsVersion;
    }

    public void setABTestsVersion(int abTestsVersion) {
        this.abTestsVersion = abTestsVersion;
    }

    /**
     * There should be no reason to call this method. Please don't use.
     * @return HashMap<String, ABTest>
     */
    public Map<String, ABTest> getAbTests() {
		return (Map<String, ABTest>)abTests;
	}

    /**
     * There should be no reason to call this method. Please don't use.
     * @param abTests
     */
    public void setAbTests(Map<String, ABTest> abTests) {
		if(abTests == null) {
			//better to throw now rather than later
			throw new NullPointerException();
		}
                this.abTests = (Serializable)abTests;
	}

    public List<String> getActiveExperimentNames() {
    	return activeExperimentNames;
    }

    public void setActiveExperimentNames(ArrayList<String> activeExperimentNames) {
    	if(activeExperimentNames == null) {
    		throw new NullPointerException();
    	}
    	this.activeExperimentNames = activeExperimentNames;
    }

    public void setConfiguredForABTests(boolean configuredForABTests) {
    	this.configuredForABTests = configuredForABTests;
    }

    public boolean isConfiguredForABTests() {
    	return configuredForABTests;
    }

	public LandingData getLandingData() {
		return landingData;
	}

	public void setLandingData(LandingData landingData) {
		this.landingData = landingData;
	}

	public HashSet<String> getLandingEventSentUrls() {
		return landingEventSentUrls;
	}

	public void setLandingEventSentUrls(HashSet<String> landingEventSentUrls) {
		this.landingEventSentUrls = landingEventSentUrls;
	}

    public String getProviderPaymentReference() {
        return providerPaymentReference;
    }

    public void setProviderPaymentReference(String providerPaymentReference) {
        this.providerPaymentReference = providerPaymentReference;
    }

    public BookingList getCrmBookings() {
        return crmBookings;
    }

    public void setCrmBookings(BookingList crmBookings) {
        this.crmBookings = crmBookings;
    }

    public void setVehicleInfoListCacheKey(String vehicleInfoListCacheKey) {
		this.vehicleInfoListCacheKey = vehicleInfoListCacheKey;
	}

    public ArrayList<String> getVehicleInfoListCacheKeyList() {
    	return vehicleInfoListCacheKeyList;
	}

    public void setVehicleInfoListCacheKeyList(ArrayList<String> vehicleInfoListCacheKeyList) {
    	this.vehicleInfoListCacheKeyList = vehicleInfoListCacheKeyList;
	}

    public void storeVehicleInfoListCacheKeyInList(String cacheKey) {
    	if (vehicleInfoListCacheKeyList == null) {
    		vehicleInfoListCacheKeyList = new ArrayList<String>();
    	}

    	if (!vehicleInfoListCacheKeyList.contains(cacheKey)) {
    		vehicleInfoListCacheKeyList.add(cacheKey);
    	}
    }


    public double getSupplierProtectionCost() {
        return supplierProtectionCost;
    }

    public void setSupplierProtectionCost(double supplierProtectionCost) {
        this.supplierProtectionCost = supplierProtectionCost;
    }

    public static void storeVehicleInfoListCacheKeyOnSession() {
        String cacheKey = ThreadLocalUtils.getRequestRQCacheKey();
        com.alltria.hirecars4u.web.common.HireCars4USession session = ConfigureSessionFilter.getSession();
        session.storeVehicleInfoListCacheKeyInList(cacheKey);
	}

    public boolean getUseCommonAffTilesDefs() {
        return BooleanUtils.toBoolean(useCommonAffTilesDefs);
    }

    public void setUseCommonAffTilesDefs(String application, String affiliateCode, String language) {
        if(useCommonAffTilesDefs==null) {
            useCommonAffTilesDefs = Boolean.parseBoolean(SystemParameterManager.getInstance().getSystemParameterValue
                    ("default", application, "default", affiliateCode, language, "useCommonAffTilesDefs", "false"));
        }
    }

	public Map<String, String> getUrlParams() {
		return urlParams;
	}

	public void setUrlParams(HashMap<String, String> urlParams) {
		this.urlParams = urlParams;
	}

	public Boolean getIsGridViewLayout() {
		return gridViewLayout;
	}

	public void setGridViewLayout(
			Boolean gridViewLayout) {
		this.gridViewLayout = gridViewLayout;
	}

	public boolean isSearchAgain() {
		return searchAgain;
	}

	public void setSearchAgain(boolean searchAgain) {
		this.searchAgain = searchAgain;
	}

	public boolean getBookingJustMade() {
		return bookingJustMade;
	}

	public void setBookingJustMade(boolean bookingJustMade) {
		this.bookingJustMade = bookingJustMade;
	}

	public boolean isAutoOptInAbg() {
		return autoOptInAbg;
	}

	public void setAutoOptInAbg(boolean autoOptInAbg) {
		this.autoOptInAbg = autoOptInAbg;
	}

    public boolean isLoyaltyControlUser() {
        return isLoyaltyControlUser;
    }

    public void setLoyaltyControlUser(boolean isLoyaltyControlUser) {
        this.isLoyaltyControlUser = isLoyaltyControlUser;
    }

	public ArrayList<String> getAlreadyVisitedCars() {
		return alreadyVisitedCars;
	}

	public void setAlreadyVisitedCars(ArrayList<String> alreadyVisitedCars) {
		this.alreadyVisitedCars = alreadyVisitedCars;
	}

    public HashMap<String, List<PricelineOpaqueCarResult>> getPricelineCachedResults() {
		return pricelineCachedResults;
	}

	public void setPricelineCachedResults(
			HashMap<String, List<PricelineOpaqueCarResult>> pricelineCachedResults) {
		this.pricelineCachedResults = pricelineCachedResults;
	}

	public String getLatestSearchCountryEnglish() {
		return latestSearchCountryEnglish;
	}

	public void setLatestSearchCountryEnglish(String latestSearchCountryEnglish) {
		this.latestSearchCountryEnglish = latestSearchCountryEnglish;
	}

	public String getLatestSearchCityEnglish() {
		return latestSearchCityEnglish;
	}

	public void setLatestSearchCityEnglish(String latestSearchCityEnglish) {
		this.latestSearchCityEnglish = latestSearchCityEnglish;
	}

	public String getLatestSearchLocationNameEnglish() {
		return latestSearchLocationNameEnglish;
	}

	public void setLatestSearchLocationNameEnglish(String latestSearchLocationNameEnglish) {
		this.latestSearchLocationNameEnglish = latestSearchLocationNameEnglish;
	}

    public String getEnabledSecretDealBetterThanBestPrice() {
        return enabledSecretDealBetterThanBestPrice;
    }

    public void setEnabledSecretDealBetterThanBestPrice(String enabledSecretDealBetterThanBestPrice) {
        this.enabledSecretDealBetterThanBestPrice = enabledSecretDealBetterThanBestPrice;
    }

    public String getEnabledSecretDealClosedUserGroup() {
        return enabledSecretDealClosedUserGroup;
    }

    public void setEnabledSecretDealClosedUserGroup(String enabledSecretDealClosedUserGroup) {
        this.enabledSecretDealClosedUserGroup = enabledSecretDealClosedUserGroup;
    }

	public String getLastViewedCarIndex() { return lastViewedCarIndex; }

	public void setLastViewedCarIndex(String lastViewedCarIndex) {
		this.lastViewedCarIndex = lastViewedCarIndex;
	}
	public String getLatestSearchType() {
		return latestSearchType;
	}

	public void setLatestSearchType(String latestSearchType) {
		this.latestSearchType = latestSearchType;
	}

    public OauthDetails getOauthDetails() {
        return oauthDetails;
    }

    public void setOauthDetails(OauthDetails oauthDetails) {
        this.oauthDetails = oauthDetails;
    }

    public LinkedHashMap<String, MemberLoyaltyInfo> getMemberLoyaltyInfoList() {
        if (memberLoyaltyInfoList == null) {
            memberLoyaltyInfoList = new LinkedHashMap<>();
        }
        return memberLoyaltyInfoList;
    }

    public void setFtsWasEnabled(boolean ftsWasEnabled) {
        if (!this.ftsWasEnabled && ftsWasEnabled) {
            this.ftsWasEnabled = ftsWasEnabled;
        }
    }

    public PPCInfo getPpcInfo() {
    	if (this.ppcInfo == null) {
	    	String placement = serverSideCookies.get(HireCars4UAction.PPC_INFO_PLACEMENT_LABEL);
			String target = serverSideCookies.get(HireCars4UAction.PPC_INFO_TARGET_LABEL);
			String param1 = serverSideCookies.get(HireCars4UAction.PPC_INFO_PARAM1_LABEL);
			String param2 = serverSideCookies.get(HireCars4UAction.PPC_INFO_PARAM2_LABEL);
			String aceId = serverSideCookies.get(HireCars4UAction.PPC_INFO_ACEID_LABEL);
			String adPosition = serverSideCookies.get(HireCars4UAction.PPC_INFO_ADPOSITION_LABEL);
			String network = serverSideCookies.get(HireCars4UAction.PPC_INFO_NETWORK_LABEL);
			String feedItemId = serverSideCookies.get(HireCars4UAction.PPC_INFO_FEEDITEMID_LABEL);
			String targetId = serverSideCookies.get(HireCars4UAction.PPC_INFO_TARGETID_LABEL);
			String locPhysicalMs = serverSideCookies.get(HireCars4UAction.PPC_INFO_LOCPHYSICALMS_LABEL);
			String locInterestMs = serverSideCookies.get(HireCars4UAction.PPC_INFO_LOCINTERESTMS_LABEL);
			String device = serverSideCookies.get(HireCars4UAction.PPC_INFO_DEVICE_LABEL);
			String deviceModel = serverSideCookies.get(HireCars4UAction.PPC_INFO_DEVICEMODEL_LABEL);
			ppcInfo = new PPCInfo(placement, target, param1, param2, aceId, adPosition, network, feedItemId, targetId, locPhysicalMs, locInterestMs, device, deviceModel);
    	}
    	return ppcInfo;
	}

    public void setLiveChatSession(LiveChatSession liveChatSession) {
        this.liveChatSession = liveChatSession;
    }

    public LiveChatSession getLiveChatSession() {
    	return liveChatSession;
	}

	public void clearLiveChatSession() {
    	liveChatSession = null;
	}

	public boolean getIsInActiveChat() {
    	return liveChatSession != null;
    }

	public void setCookiePolicy(CookiePolicy cookiePolicy) {
		this.cookiePolicy = cookiePolicy;
	}

	public CookiePolicy getCookiePolicy() {
		return cookiePolicy;
	}
}