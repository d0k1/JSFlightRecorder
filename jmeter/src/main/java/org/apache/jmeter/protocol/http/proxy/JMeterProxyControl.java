package org.apache.jmeter.protocol.http.proxy;

/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */

import com.focusit.jsflight.jmeter.JMeterRecorder;
import org.apache.commons.codec.binary.Base64;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.RandomStringUtils;
import org.apache.commons.lang3.time.DateUtils;
import org.apache.http.conn.ssl.AbstractVerifier;
import org.apache.jmeter.config.Arguments;
import org.apache.jmeter.config.ConfigElement;
import org.apache.jmeter.config.ConfigTestElement;
import org.apache.jmeter.control.GenericController;
import org.apache.jmeter.engine.util.ValueReplacer;
import org.apache.jmeter.functions.InvalidVariableException;
import org.apache.jmeter.gui.tree.JMeterTreeNode;
import org.apache.jmeter.protocol.http.control.AuthManager;
import org.apache.jmeter.protocol.http.control.Authorization;
import org.apache.jmeter.protocol.http.control.Header;
import org.apache.jmeter.protocol.http.control.HeaderManager;
import org.apache.jmeter.protocol.http.sampler.HTTPSampleResult;
import org.apache.jmeter.protocol.http.sampler.HTTPSamplerBase;
import org.apache.jmeter.protocol.http.sampler.HTTPSamplerFactory;
import org.apache.jmeter.protocol.http.util.HTTPConstants;
import org.apache.jmeter.samplers.SampleResult;
import org.apache.jmeter.testelement.TestElement;
import org.apache.jmeter.testelement.property.*;
import org.apache.jmeter.util.JMeterUtils;
import org.apache.jorphan.exec.KeyToolUtils;
import org.apache.jorphan.util.JOrphanUtils;
import org.apache.oro.text.MalformedCachePatternException;
import org.apache.oro.text.regex.Pattern;
import org.apache.oro.text.regex.Perl5Compiler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.net.MalformedURLException;
import java.security.GeneralSecurityException;
import java.security.KeyStore;
import java.security.UnrecoverableKeyException;
import java.security.cert.X509Certificate;
import java.util.*;
import java.util.prefs.Preferences;

//For unit tests, @see TestProxyControl

/**
 * Copy paste of org.apache.jmeter.protocol.http.proxy.ProxyControl
 */
@SuppressWarnings({"unused", "unchecked"})
public class JMeterProxyControl extends GenericController
{

    public static final int DEFAULT_PORT = 8080;
    private static final Logger LOG = LoggerFactory.getLogger(JMeterProxyControl.class);
    private static final long serialVersionUID = 240L;
    //+ JMX file attributes
    private static final String PORT = "ProxyControlGui.port"; // $NON-NLS-1$

    private static final String DOMAINS = "ProxyControlGui.domains"; // $NON-NLS-1$

    private static final String EXCLUDE_LIST = "ProxyControlGui.exclude_list"; // $NON-NLS-1$

    private static final String INCLUDE_LIST = "ProxyControlGui.include_list"; // $NON-NLS-1$

    private static final String CAPTURE_HTTP_HEADERS = "ProxyControlGui.capture_http_headers"; // $NON-NLS-1$

    private static final String ADD_ASSERTIONS = "ProxyControlGui.add_assertion"; // $NON-NLS-1$

    private static final String GROUPING_MODE = "ProxyControlGui.grouping_mode"; // $NON-NLS-1$

    private static final String SAMPLER_TYPE_NAME = "ProxyControlGui.sampler_type_name"; // $NON-NLS-1$

    private static final String SAMPLER_REDIRECT_AUTOMATICALLY = "ProxyControlGui.sampler_redirect_automatically"; // $NON-NLS-1$

    private static final String SAMPLER_FOLLOW_REDIRECTS = "ProxyControlGui.sampler_follow_redirects"; // $NON-NLS-1$

    private static final String USE_KEEPALIVE = "ProxyControlGui.use_keepalive"; // $NON-NLS-1$

    private static final String SAMPLER_DOWNLOAD_IMAGES = "ProxyControlGui.sampler_download_images"; // $NON-NLS-1$

    private static final String REGEX_MATCH = "ProxyControlGui.regex_match"; // $NON-NLS-1$

    private static final String CONTENT_TYPE_EXCLUDE = "ProxyControlGui.content_type_exclude"; // $NON-NLS-1$

    private static final String CONTENT_TYPE_INCLUDE = "ProxyControlGui.content_type_include"; // $NON-NLS-1$

    private static final String NOTIFY_CHILD_SAMPLER_LISTENERS_FILTERED = "ProxyControlGui.notify_child_sl_filtered"; // $NON-NLS-1$

    private static final String BASIC_AUTH = "Basic"; // $NON-NLS-1$

    private static final String DIGEST_AUTH = "Digest"; // $NON-NLS-1$

    //- JMX file attributes

    // Original numeric order (we now use strings)
    private static final String SAMPLER_TYPE_HTTP_SAMPLER_JAVA = "0";
    private static final String SAMPLER_TYPE_HTTP_SAMPLER_HC3_1 = "1";
    private static final String SAMPLER_TYPE_HTTP_SAMPLER_HC4 = "2";

    // for ssl connection
    private static final String KEYSTORE_TYPE = JMeterUtils.getPropDefault("proxy.cert.type", "JKS"); // $NON-NLS-1$ $NON-NLS-2$

    // Proxy configuration SSL
    private static final String CERT_DIRECTORY = JMeterUtils.getPropDefault("proxy.cert.directory",
            JMeterUtils.getJMeterBinDir()); // $NON-NLS-1$

    private static final String CERT_FILE_DEFAULT = "proxyserver.jks";// $NON-NLS-1$

    private static final String CERT_FILE = JMeterUtils.getPropDefault("proxy.cert.file", CERT_FILE_DEFAULT); // $NON-NLS-1$

    private static final File CERT_PATH = new File(CERT_DIRECTORY, CERT_FILE);

    private static final String CERT_PATH_ABS = CERT_PATH.getAbsolutePath();

    private static final String DEFAULT_PASSWORD = "password"; // $NON-NLS-1$

    // Keys for user preferences
    private static final String USER_PASSWORD_KEY = "proxy_cert_password";

    private static final Preferences PREFERENCES = Preferences.userNodeForPackage(JMeterProxyControl.class);
    // Note: Windows user preferences are stored relative to: HKEY_CURRENT_USER\Software\JavaSoft\Prefs

    // Whether to use dymanic key generation (if supported)
    private static final boolean USE_DYNAMIC_KEYS = JMeterUtils.getPropDefault("proxy.cert.dynamic_keys", true); // $NON-NLS-1$;

    // The alias to be used if dynamic host names are not possible
    private static final String JMETER_SERVER_ALIAS = ":jmeter:"; // $NON-NLS-1$

    private static final int CERT_VALIDITY = JMeterUtils.getPropDefault("proxy.cert.validity", 7); // $NON-NLS-1$

    // If this is defined, it is assumed to be the alias of a user-supplied certificate; overrides dynamic mode
    private static final String CERT_ALIAS = JMeterUtils.getProperty("proxy.cert.alias"); // $NON-NLS-1$
    private static final KeystoreMode KEYSTORE_MODE;
    // Whether to use the redirect disabling feature (can be switched off if it does not work)
    private static final boolean ATTEMPT_REDIRECT_DISABLING = JMeterUtils.getPropDefault("proxy.redirect.disabling",
            true); // $NON-NLS-1$
    // Although this field is mutable, it is only accessed within the synchronized method deliverSampler()
    private static String LAST_REDIRECT = null;

    static
    {
        if (CERT_ALIAS != null)
        {
            KEYSTORE_MODE = KeystoreMode.USER_KEYSTORE;
            LOG.info("HTTP(S) Test Script Recorder will use the keystore '" + CERT_PATH_ABS + "' with the alias: '"
                    + CERT_ALIAS + "'");
        }
        else
        {
            if (!KeyToolUtils.haveKeytool())
            {
                KEYSTORE_MODE = KeystoreMode.NONE;
            }
            else if (USE_DYNAMIC_KEYS)
            {
                KEYSTORE_MODE = KeystoreMode.DYNAMIC_KEYSTORE;
                LOG.info("HTTP(S) Test Script Recorder SSL Proxy will use keys that support embedded 3rd party resources in file "
                        + CERT_PATH_ABS);
            }
            else
            {
                KEYSTORE_MODE = KeystoreMode.JMETER_KEYSTORE;
                LOG.warn("HTTP(S) Test Script Recorder SSL Proxy will use keys that may not work for embedded resources in file "
                        + CERT_PATH_ABS);
            }
        }
    }

    private final JMeterRecorder recorder;
    private transient JMeterDaemon server;
    private transient int currentSamplerIndex = 0;
    private long maxSamplersPerJmx;
    private long lastTime = 0;// When was the last sample seen?
    /*
     * TODO this assumes that the redirected response will always immediately follow the original response.
     * This may not always be true.
     * Is there a better way to do this?
     */
    private transient KeyStore sslKeyStore;
    private volatile boolean samplerRedirectAutomatically = false;
    private volatile boolean samplerFollowRedirects = false;
    private volatile boolean useKeepAlive = false;
    private volatile boolean samplerDownloadImages = false;
    private volatile boolean notifyChildSamplerListenersOfFilteredSamples = true;
    private volatile boolean regexMatch = false;// Should we match using regexes?
    /**
     * Tree node where the samples should be stored.
     * <p>
     * This property is not persistent.
     */
    private TestElement target;
    private String storePassword;
    private String keyPassword;

    public JMeterProxyControl(JMeterRecorder jMeterRecorder, Long maxRequestsPerScenario)
    {
        setPort(DEFAULT_PORT);
        setExcludeList(new HashSet<>());
        setIncludeList(new HashSet<>());
        setCaptureHttpHeaders(true); // maintain original behaviour
        this.recorder = jMeterRecorder;
        this.maxSamplersPerJmx = maxRequestsPerScenario;
    }

    public static boolean isDynamicMode()
    {
        return KEYSTORE_MODE == KeystoreMode.DYNAMIC_KEYSTORE;
    }

    public JMeterRecorder getRecorder()
    {
        return recorder;
    }

    public void setPort(int port)
    {
        this.setProperty(new IntegerProperty(PORT, port));
    }

    public String getSslDomains()
    {
        return getPropertyAsString(DOMAINS, "");
    }

    public void setSslDomains(String domains)
    {
        setProperty(DOMAINS, domains, "");
    }

    @Deprecated
    public void setSamplerTypeName(int samplerTypeName)
    {
        setProperty(new IntegerProperty(SAMPLER_TYPE_NAME, samplerTypeName));
    }

    /**
     * @param b flag whether keep alive should be used
     */
    public void setUseKeepAlive(boolean b)
    {
        useKeepAlive = b;
        setProperty(new BooleanProperty(USE_KEEPALIVE, b));
    }

    public void setIncludeList(Collection<String> list)
    {
        setProperty(new CollectionProperty(INCLUDE_LIST, new HashSet<>(list)));
    }

    public void setExcludeList(Collection<String> list)
    {
        setProperty(new CollectionProperty(EXCLUDE_LIST, new HashSet<>(list)));
    }

    public boolean getAssertions()
    {
        return getPropertyAsBoolean(ADD_ASSERTIONS);
    }

    public void setAssertions(boolean b)
    {
        setProperty(new BooleanProperty(ADD_ASSERTIONS, b));
    }

    public int getGroupingMode()
    {
        return getPropertyAsInt(GROUPING_MODE);
    }

    public void setGroupingMode(int grouping)
    {
        setProperty(new IntegerProperty(GROUPING_MODE, grouping));
    }

    public int getPort()
    {
        return getPropertyAsInt(PORT);
    }

    public void setPort(String port)
    {
        setProperty(PORT, port);
    }

    public String getPortString()
    {
        return getPropertyAsString(PORT);
    }

    public int getDefaultPort()
    {
        return DEFAULT_PORT;
    }

    public boolean getCaptureHttpHeaders()
    {
        return getPropertyAsBoolean(CAPTURE_HTTP_HEADERS);
    }

    public void setCaptureHttpHeaders(boolean capture)
    {
        setProperty(new BooleanProperty(CAPTURE_HTTP_HEADERS, capture));
    }

    public String getSamplerTypeName()
    {
        // Convert the old numeric types - just in case someone wants to reload the workbench
        String type = getPropertyAsString(SAMPLER_TYPE_NAME);
        if (SAMPLER_TYPE_HTTP_SAMPLER_JAVA.equals(type))
        {
            type = HTTPSamplerFactory.IMPL_JAVA;
        }
        else if (SAMPLER_TYPE_HTTP_SAMPLER_HC3_1.equals(type))
        {
            type = HTTPSamplerFactory.IMPL_HTTP_CLIENT3_1;
        }
        else if (SAMPLER_TYPE_HTTP_SAMPLER_HC4.equals(type))
        {
            type = HTTPSamplerFactory.IMPL_HTTP_CLIENT4;
        }
        return type;
    }

    public void setSamplerTypeName(String samplerTypeName)
    {
        setProperty(new StringProperty(SAMPLER_TYPE_NAME, samplerTypeName));
    }

    public boolean getSamplerRedirectAutomatically()
    {
        return getPropertyAsBoolean(SAMPLER_REDIRECT_AUTOMATICALLY, false);
    }

    public void setSamplerRedirectAutomatically(boolean b)
    {
        samplerRedirectAutomatically = b;
        setProperty(new BooleanProperty(SAMPLER_REDIRECT_AUTOMATICALLY, b));
    }

    public boolean getSamplerFollowRedirects()
    {
        return getPropertyAsBoolean(SAMPLER_FOLLOW_REDIRECTS, true);
    }

    public void setSamplerFollowRedirects(boolean b)
    {
        samplerFollowRedirects = b;
        setProperty(new BooleanProperty(SAMPLER_FOLLOW_REDIRECTS, b));
    }

    public boolean getUseKeepalive()
    {
        return getPropertyAsBoolean(USE_KEEPALIVE, true);
    }

    public boolean getSamplerDownloadImages()
    {
        return getPropertyAsBoolean(SAMPLER_DOWNLOAD_IMAGES, false);
    }

    public void setSamplerDownloadImages(boolean b)
    {
        samplerDownloadImages = b;
        setProperty(new BooleanProperty(SAMPLER_DOWNLOAD_IMAGES, b));
    }

    public boolean getNotifyChildSamplerListenerOfFilteredSamplers()
    {
        return getPropertyAsBoolean(NOTIFY_CHILD_SAMPLER_LISTENERS_FILTERED, true);
    }

    public void setNotifyChildSamplerListenerOfFilteredSamplers(boolean b)
    {
        notifyChildSamplerListenersOfFilteredSamples = b;
        setProperty(new BooleanProperty(NOTIFY_CHILD_SAMPLER_LISTENERS_FILTERED, b));
    }

    public boolean getRegexMatch()
    {
        return getPropertyAsBoolean(REGEX_MATCH, false);
    }

    /**
     * @param b flag whether regex matching should be used
     */
    public void setRegexMatch(boolean b)
    {
        regexMatch = b;
        setProperty(new BooleanProperty(REGEX_MATCH, b));
    }

    public String getContentTypeExclude()
    {
        return getPropertyAsString(CONTENT_TYPE_EXCLUDE);
    }

    public void setContentTypeExclude(String contentTypeExclude)
    {
        setProperty(new StringProperty(CONTENT_TYPE_EXCLUDE, contentTypeExclude));
    }

    public String getContentTypeInclude()
    {
        return getPropertyAsString(CONTENT_TYPE_INCLUDE);
    }

    public void setContentTypeInclude(String contentTypeInclude)
    {
        setProperty(new StringProperty(CONTENT_TYPE_INCLUDE, contentTypeInclude));
    }

    public void addConfigElement(ConfigElement config)
    {
        // NOOP
    }

    public void startProxy() throws IOException
    {
        try
        {
            initKeyStore();
        }
        catch (GeneralSecurityException e)
        {
            LOG.error("Could not initialise key store", e);
            throw new IOException("Could not create keystore", e);
        }
        catch (IOException e)
        { // make sure we LOG the error
            LOG.error("Could not initialise key store", e);
            throw e;
        }
        //        notifyTestListenersOfStart();
        try
        {
            server = new JMeterDaemon(getPort(), this);
            server.start();
            //            GuiPackage.getInstance().register(server);
        }
        catch (IOException e)
        {
            LOG.error("Could not create Proxy daemon", e);
            throw e;
        }
    }

    public void addExcludedPattern(String pattern)
    {
        getExcludePatterns().addItem(pattern);
    }

    public CollectionProperty getExcludePatterns()
    {
        return (CollectionProperty)getProperty(EXCLUDE_LIST);
    }

    public void addIncludedPattern(String pattern)
    {
        getIncludePatterns().addItem(pattern);
    }

    public CollectionProperty getIncludePatterns()
    {
        return (CollectionProperty)getProperty(INCLUDE_LIST);
    }

    public void clearExcludedPatterns()
    {
        getExcludePatterns().clear();
    }

    public void clearIncludedPatterns()
    {
        getIncludePatterns().clear();
    }

    /**
     * @return the target fileconfigholder node
     */
    public JMeterTreeNode getTarget()
    {
        return null;
    }

    /**
     * Sets the target node where the samples generated by the proxy have to be
     * stored.
     *
     * @param target target node to store generated samples
     */
    public void setTarget(JMeterTreeNode target)
    {
    }

    public void setTargetTestElement(TestElement target)
    {
        this.target = target;
    }

    /**
     * Receives the recorded sampler from the proxy server for placing in the
     * test tree; this is skipped if the sampler is null (e.g. for recording SSL errors)
     * Always sends the result to any registered sample listeners.
     *
     * @param sampler    the sampler, may be null
     * @param subConfigs the configuration elements to be added (e.g. header namager)
     * @param result     the sample result, not null
     *                   TODO param serverResponse to be added to allow saving of the
     *                   server's response while recording.
     */
    public synchronized void deliverSampler(final HTTPSamplerBase sampler, final TestElement[] subConfigs,
            final SampleResult result)
    {
        if (sampler != null)
        {
            if (ATTEMPT_REDIRECT_DISABLING && (samplerRedirectAutomatically || samplerFollowRedirects))
            {
                if (result instanceof HTTPSampleResult)
                {
                    final HTTPSampleResult httpSampleResult = (HTTPSampleResult)result;
                    final String urlAsString = httpSampleResult.getUrlAsString();
                    if (urlAsString.equals(LAST_REDIRECT))
                    { // the url matches the last redirect
                        sampler.setEnabled(false);
                        sampler.setComment("Detected a redirect from the previous sample");
                    }
                    else
                    { // this is not the result of a redirect
                        LAST_REDIRECT = null; // so break the chain
                    }
                    if (httpSampleResult.isRedirect())
                    { // Save Location so resulting sample can be disabled
                        if (LAST_REDIRECT == null)
                        {
                            sampler.setComment("Detected the start of a redirect chain");
                        }
                        LAST_REDIRECT = httpSampleResult.getRedirectLocation();
                    }
                    else
                    {
                        LAST_REDIRECT = null;
                    }
                }
            }
            if (filterContentType(result) && filterUrl(sampler))
            {
                if (currentSamplerIndex >= maxSamplersPerJmx)
                {
                    recorder.splitScenario();
                    currentSamplerIndex = 0;
                }
                TestElement myTarget = target;
                sampler.setAutoRedirects(samplerRedirectAutomatically);
                sampler.setFollowRedirects(samplerFollowRedirects);
                sampler.setUseKeepAlive(useKeepAlive);
                sampler.setImageParser(samplerDownloadImages);

                Authorization authorization = createAuthorization(subConfigs, sampler);

                placeSampler(sampler, subConfigs, myTarget);
                currentSamplerIndex++;
            }
            else
            {
                if (LOG.isDebugEnabled())
                {
                    LOG.debug("Sample excluded based on url or content-type: " + result.getUrlAsString() + " - "
                            + result.getContentType());
                }
                result.setSampleLabel("[" + result.getSampleLabel() + "]");
            }
        }
    }

    /**
     * Detect Header manager in subConfigs,
     * Find(if any) Authorization header
     * Construct Authentication object
     * Removes Authorization if present
     *
     * @param subConfigs {@link TestElement}[]
     * @param sampler    {@link HTTPSamplerBase}
     * @return {@link Authorization}
     */
    private Authorization createAuthorization(final TestElement[] subConfigs, HTTPSamplerBase sampler)
    {
        Header authHeader;
        Authorization authorization = null;
        // Iterate over subconfig elements searching for HeaderManager
        for (TestElement te : subConfigs)
        {
            if (te instanceof HeaderManager)
            {
                List<TestElementProperty> headers = (ArrayList<TestElementProperty>)((HeaderManager)te).getHeaders()
                        .getObjectValue();
                for (Iterator<?> iterator = headers.iterator(); iterator.hasNext();)
                {
                    TestElementProperty tep = (TestElementProperty)iterator.next();
                    if (tep.getName().equals(HTTPConstants.HEADER_AUTHORIZATION))
                    {
                        //Construct Authorization object from HEADER_AUTHORIZATION
                        authHeader = (Header)tep.getObjectValue();
                        String[] authHeaderContent = authHeader.getValue().split(" ");//$NON-NLS-1$
                        String authType = null;
                        String authCredentialsBase64;
                        if (authHeaderContent.length >= 2)
                        {
                            authType = authHeaderContent[0];
                            authCredentialsBase64 = authHeaderContent[1];
                            authorization = new Authorization();
                            try
                            {
                                authorization.setURL(sampler.getUrl().toExternalForm());
                            }
                            catch (MalformedURLException e)
                            {
                                LOG.error("Error filling url on authorization, message:" + e.getMessage(), e);
                                authorization.setURL("${AUTH_BASE_URL}");//$NON-NLS-1$
                            }
                            // if HEADER_AUTHORIZATION contains "Basic"
                            // then set Mechanism.BASIC_DIGEST, otherwise Mechanism.KERBEROS
                            authorization
                                    .setMechanism(authType.equals(BASIC_AUTH) || authType.equals(DIGEST_AUTH) ? AuthManager.Mechanism.BASIC_DIGEST
                                            : AuthManager.Mechanism.KERBEROS);
                            if (BASIC_AUTH.equals(authType))
                            {
                                String authCred = new String(Base64.decodeBase64(authCredentialsBase64));
                                String[] loginPassword = authCred.split(":"); //$NON-NLS-1$
                                authorization.setUser(loginPassword[0]);
                                authorization.setPass(loginPassword[1]);
                            }
                            else
                            {
                                // Digest or Kerberos
                                authorization.setUser("${AUTH_LOGIN}");//$NON-NLS-1$
                                authorization.setPass("${AUTH_PASSWORD}");//$NON-NLS-1$

                            }
                        }
                        // remove HEADER_AUTHORIZATION from HeaderManager 
                        // because it's useless after creating Authorization object
                        iterator.remove();
                    }
                }
            }
        }
        return authorization;
    }

    public void stopProxy()
    {
        if (server != null)
        {
            server.stopServer();
            //            GuiPackage.getInstance().unregister(server);
            try
            {
                server.join(1000); // wait for server to stop
            }
            catch (InterruptedException e)
            {
                //NOOP
            }
            //            notifyTestListenersOfEnd();
            server = null;
        }
    }

    public String[] getCertificateDetails()
    {
        if (isDynamicMode())
        {
            try
            {
                X509Certificate caCert = (X509Certificate)sslKeyStore.getCertificate(KeyToolUtils.getRootCAalias());
                if (caCert == null)
                {
                    return new String[] { "Could not find certificate" };
                }
                return new String[] { caCert.getSubjectX500Principal().toString(),
                        "Fingerprint(SHA1): " + JOrphanUtils.baToHexString(DigestUtils.sha1(caCert.getEncoded()), ' '),
                        "Created: " + caCert.getNotBefore().toString() };
            }
            catch (GeneralSecurityException e)
            {
                LOG.error("Problem reading root CA from keystore", e);
                return new String[] { "Problem with root certificate", e.getMessage() };
            }
        }
        return null; // should not happen
    }

    // Package protected to allow test case access
    private boolean filterUrl(HTTPSamplerBase sampler)
    {
        String domain = sampler.getDomain();
        if (domain == null || domain.length() == 0)
        {
            return false;
        }

        String url = generateMatchUrl(sampler);
        CollectionProperty includePatterns = getIncludePatterns();
        if (includePatterns.size() > 0)
        {
            if (!matchesPatterns(url, includePatterns))
            {
                return false;
            }
        }

        CollectionProperty excludePatterns = getExcludePatterns();
        if (excludePatterns.size() > 0)
        {
            if (matchesPatterns(url, excludePatterns))
            {
                return false;
            }
        }

        return true;
    }

    // Package protected to allow test case access

    /**
     * Filter the response based on the content type.
     * If no include nor exclude filter is specified, the result will be included
     *
     * @param result the sample result to check
     * @return <code>true</code> means result will be kept
     */
    private boolean filterContentType(SampleResult result)
    {
        String includeExp = getContentTypeInclude();
        String excludeExp = getContentTypeExclude();
        // If no expressions are specified, we let the sample pass
        if ((includeExp == null || includeExp.length() == 0) && (excludeExp == null || excludeExp.length() == 0))
        {
            return true;
        }

        // Check that we have a content type
        String sampleContentType = result.getContentType();
        if (sampleContentType == null || sampleContentType.length() == 0)
        {
            if (LOG.isDebugEnabled())
            {
                LOG.debug("No Content-type found for : " + result.getUrlAsString());
            }

            return true;
        }

        if (LOG.isDebugEnabled())
        {
            LOG.debug("Content-type to filter : " + sampleContentType);
        }

        return testPattern(includeExp, sampleContentType, true) && testPattern(excludeExp, sampleContentType, false);
    }

    /**
     * Returns true if matching pattern was different from expectedToMatch
     *
     * @param expression        Expression to match
     * @param sampleContentType
     * @return boolean true if Matching expression
     */
    private boolean testPattern(String expression, String sampleContentType, boolean expectedToMatch)
    {
        if (expression != null && expression.length() > 0)
        {
            if (LOG.isDebugEnabled())
            {
                LOG.debug("Testing Expression : " + expression + " on sampleContentType:" + sampleContentType
                        + ", expected to match:" + expectedToMatch);
            }

            Pattern pattern;
            try
            {
                pattern = JMeterUtils.getPatternCache().getPattern(expression,
                        Perl5Compiler.READ_ONLY_MASK | Perl5Compiler.SINGLELINE_MASK);
                if (JMeterUtils.getMatcher().contains(sampleContentType, pattern) != expectedToMatch)
                {
                    return false;
                }
            }
            catch (MalformedCachePatternException e)
            {
                LOG.warn("Skipped invalid content pattern: " + expression, e);
            }
        }
        return true;
    }

    private void placeSampler(final HTTPSamplerBase sampler, final TestElement[] subConfigs, final TestElement myTarget)
    {
        try
        {
            JMeterUtils.runSafe(() -> myTarget.addTestElement(sampler));
        }
        catch (Exception e)
        {
            JMeterUtils.reportErrorToUser(e.getMessage());
        }
    }

    /**
     * Remove from the sampler all values which match the one provided by the
     * first configuration in the given collection which provides a value for
     * that property.
     *
     * @param sampler        Sampler to remove values from.
     * @param configurations ConfigTestElements in descending priority.
     */
    private void removeValuesFromSampler(HTTPSamplerBase sampler, Collection<ConfigTestElement> configurations)
    {
        for (PropertyIterator props = sampler.propertyIterator(); props.hasNext();)
        {
            JMeterProperty prop = props.next();
            String name = prop.getName();
            String value = prop.getStringValue();

            // There's a few properties which are excluded from this processing:
            if (name.equals(TestElement.ENABLED) || name.equals(TestElement.GUI_CLASS) || name.equals(TestElement.NAME)
                    || name.equals(TestElement.TEST_CLASS))
            {
                continue; // go on with next property.
            }

            for (ConfigTestElement config : configurations)
            {
                String configValue = config.getPropertyAsString(name);

                if (configValue != null && configValue.length() > 0)
                {
                    if (configValue.equals(value))
                    {
                        sampler.setProperty(name, ""); // $NON-NLS-1$
                    }
                    // Property was found in a config element. Whether or not
                    // it matched the value in the sampler, we're done with
                    // this property -- don't look at lower-priority configs:
                    break;
                }
            }
        }
    }

    private String generateMatchUrl(HTTPSamplerBase sampler)
    {
        StringBuilder buf = new StringBuilder(sampler.getDomain());
        buf.append(':'); // $NON-NLS-1$
        buf.append(sampler.getPort());
        buf.append(sampler.getPath());
        if (sampler.getQueryString().length() > 0)
        {
            buf.append('?'); // $NON-NLS-1$
            buf.append(sampler.getQueryString());
        }
        return buf.toString();
    }

    private boolean matchesPatterns(String url, CollectionProperty patterns)
    {
        for (JMeterProperty jMeterProperty : patterns)
        {
            String item = jMeterProperty.getStringValue();
            try
            {
                Pattern pattern = JMeterUtils.getPatternCache().getPattern(item,
                        Perl5Compiler.READ_ONLY_MASK | Perl5Compiler.SINGLELINE_MASK);
                if (JMeterUtils.getMatcher().matches(url, pattern))
                {
                    return true;
                }
            }
            catch (MalformedCachePatternException e)
            {
                LOG.warn("Skipped invalid pattern: " + item, e);
            }
        }
        return false;
    }

    /**
     * Scan all test elements passed in for values matching the value of any of
     * the variables in any of the variable-holding elements in the collection.
     *
     * @param sampler   A TestElement to replace values on
     * @param configs   More TestElements to replace values on
     * @param variables Collection of Arguments to use to do the replacement, ordered
     *                  by ascending priority.
     */
    private void replaceValues(TestElement sampler, TestElement[] configs, Collection<Arguments> variables)
    {
        // Build the replacer from all the variables in the collection:
        ValueReplacer replacer = new ValueReplacer();
        for (Arguments variable : variables)
        {
            final Map<String, String> map = variable.getArgumentsAsMap();
            for (Iterator<String> vals = map.values().iterator(); vals.hasNext();)
            {
                final Object next = vals.next();
                if ("".equals(next))
                {// Drop any empty values (Bug 45199)
                    vals.remove();
                }
            }
            replacer.addVariables(map);
        }

        try
        {
            boolean cachedRegexpMatch = regexMatch;
            replacer.reverseReplace(sampler, cachedRegexpMatch);
            for (TestElement config : configs)
            {
                if (config != null)
                {
                    replacer.reverseReplace(config, cachedRegexpMatch);
                }
            }
        }
        catch (InvalidVariableException e)
        {
            LOG.warn("Invalid variables included for replacement into recorded " + "sample", e);
        }
    }

    @Override
    public boolean canRemove()
    {
        return null == server;
    }

    private void initKeyStore() throws IOException, GeneralSecurityException
    {
        switch (KEYSTORE_MODE)
        {
        case DYNAMIC_KEYSTORE:
            storePassword = getPassword();
            keyPassword = getPassword();
            initDynamicKeyStore();
            break;
        case JMETER_KEYSTORE:
            storePassword = getPassword();
            keyPassword = getPassword();
            initJMeterKeyStore();
            break;
        case USER_KEYSTORE:
            storePassword = JMeterUtils.getPropDefault("proxy.cert.keystorepass", DEFAULT_PASSWORD); // $NON-NLS-1$;
            keyPassword = JMeterUtils.getPropDefault("proxy.cert.keypassword", DEFAULT_PASSWORD); // $NON-NLS-1$;
            LOG.info("HTTP(S) Test Script Recorder will use the keystore '" + CERT_PATH_ABS + "' with the alias: '"
                    + CERT_ALIAS + "'");
            initUserKeyStore();
            break;
        case NONE:
            throw new IOException("Cannot find keytool application and no keystore was provided");
        default:
            throw new IllegalStateException("Impossible case: " + KEYSTORE_MODE);
        }
    }

    /**
     * Initialise the user-provided keystore
     */
    private void initUserKeyStore()
    {
        try
        {
            sslKeyStore = getKeyStore(storePassword.toCharArray());
            X509Certificate caCert = (X509Certificate)sslKeyStore.getCertificate(CERT_ALIAS);
            if (caCert == null)
            {
                LOG.error("Could not find key with alias " + CERT_ALIAS);
                sslKeyStore = null;
            }
            else
            {
                caCert.checkValidity(new Date(System.currentTimeMillis() + DateUtils.MILLIS_PER_DAY));
            }
        }
        catch (Exception e)
        {
            sslKeyStore = null;
            LOG.error("Could not open keystore or certificate is not valid " + CERT_PATH_ABS + " " + e.getMessage());
        }
    }

    /**
     * Initialise the dynamic domain keystore
     */
    private void initDynamicKeyStore() throws IOException, GeneralSecurityException
    {
        if (storePassword != null)
        { // Assume we have already created the store
            try
            {
                sslKeyStore = getKeyStore(storePassword.toCharArray());
                for (String alias : KeyToolUtils.getCAaliases())
                {
                    X509Certificate caCert = (X509Certificate)sslKeyStore.getCertificate(alias);
                    if (caCert == null)
                    {
                        sslKeyStore = null; // no CA key - probably the wrong store type.
                        break; // cannot continue
                    }
                    else
                    {
                        caCert.checkValidity(new Date(System.currentTimeMillis() + DateUtils.MILLIS_PER_DAY));
                        LOG.info("Valid alias found for " + alias);
                    }
                }
            }
            catch (IOException e)
            { // store is faulty, we need to recreate it
                sslKeyStore = null; // if cert is not valid, flag up to recreate it
                if (e.getCause() instanceof UnrecoverableKeyException)
                {
                    LOG.warn("Could not read key store " + e.getMessage() + "; cause: " + e.getCause().getMessage());
                }
                else
                {
                    LOG.warn("Could not open/read key store " + e.getMessage()); // message includes the file name
                }
            }
            catch (GeneralSecurityException e)
            {
                sslKeyStore = null; // if cert is not valid, flag up to recreate it
                LOG.warn("Problem reading key store: " + e.getMessage());
            }
        }
        if (sslKeyStore == null)
        { // no existing file or not valid
            storePassword = RandomStringUtils.randomAlphanumeric(20); // Alphanum to avoid issues with command-line quoting
            keyPassword = storePassword; // we use same password for both
            setPassword(storePassword);
            LOG.info("Creating Proxy CA in " + CERT_PATH_ABS);
            KeyToolUtils.generateProxyCA(CERT_PATH, storePassword, CERT_VALIDITY);
            LOG.info("Created keystore in " + CERT_PATH_ABS);
            sslKeyStore = getKeyStore(storePassword.toCharArray()); // This should now work
        }
        final String sslDomains = getSslDomains().trim();
        if (sslDomains.length() > 0)
        {
            final String[] domains = sslDomains.split(",");
            // The subject may be either a host or a domain
            for (String subject : domains)
            {
                if (isValid(subject))
                {
                    if (!sslKeyStore.containsAlias(subject))
                    {
                        LOG.info("Creating entry " + subject + " in " + CERT_PATH_ABS);
                        KeyToolUtils.generateHostCert(CERT_PATH, storePassword, subject, CERT_VALIDITY);
                        sslKeyStore = getKeyStore(storePassword.toCharArray()); // reload to pick up new aliases
                        // reloading is very quick compared with creating an entry currently
                    }
                }
                else
                {
                    LOG.warn("Attempt to create an invalid domain certificate: " + subject);
                }
            }
        }
    }

    private boolean isValid(String subject) {
        String parts[] = subject.split("\\.");
        // not a wildcard
        return !parts[0].endsWith("*") || parts.length >= 3 && AbstractVerifier.acceptableCountryWildcard(subject);
    }

    // This should only be called for a specific host
    public KeyStore updateKeyStore(String port, String host) throws IOException, GeneralSecurityException
    {
        synchronized (CERT_PATH)
        { // ensure Proxy threads cannot interfere with each other
            if (!sslKeyStore.containsAlias(host))
            {
                LOG.info(port + "Creating entry " + host + " in " + CERT_PATH_ABS);
                KeyToolUtils.generateHostCert(CERT_PATH, storePassword, host, CERT_VALIDITY);
            }
            sslKeyStore = getKeyStore(storePassword.toCharArray()); // reload after adding alias
        }
        return sslKeyStore;
    }

    /**
     * Initialise the single key JMeter keystore (original behaviour)
     */
    private void initJMeterKeyStore() throws IOException, GeneralSecurityException
    {
        if (storePassword != null)
        { // Assume we have already created the store
            try
            {
                sslKeyStore = getKeyStore(storePassword.toCharArray());
                X509Certificate caCert = (X509Certificate)sslKeyStore.getCertificate(JMETER_SERVER_ALIAS);
                caCert.checkValidity(new Date(System.currentTimeMillis() + DateUtils.MILLIS_PER_DAY));
            }
            catch (Exception e)
            { // store is faulty, we need to recreate it
                sslKeyStore = null; // if cert is not valid, flag up to recreate it
                LOG.warn("Could not open expected file or certificate is not valid " + CERT_PATH_ABS + " "
                        + e.getMessage());
            }
        }
        if (sslKeyStore == null)
        { // no existing file or not valid
            storePassword = RandomStringUtils.randomAlphanumeric(20); // Alphanum to avoid issues with command-line quoting
            keyPassword = storePassword; // we use same password for both
            setPassword(storePassword);
            LOG.info("Generating standard keypair in " + CERT_PATH_ABS);
            if (!CERT_PATH.delete())
            { // safer to start afresh
                LOG.warn("Could not delete " + CERT_PATH.getAbsolutePath()
                        + ", this could create issues, stop jmeter, ensure file is deleted and restart again");
            }
            KeyToolUtils.genkeypair(CERT_PATH, JMETER_SERVER_ALIAS, storePassword, CERT_VALIDITY, null, null);
            sslKeyStore = getKeyStore(storePassword.toCharArray()); // This should now work
        }
    }

    private KeyStore getKeyStore(char[] password) throws GeneralSecurityException, IOException
    {
        InputStream in = null;
        try
        {
            in = new BufferedInputStream(new FileInputStream(CERT_PATH));
            LOG.debug("Opened Keystore file: " + CERT_PATH_ABS);
            KeyStore ks = KeyStore.getInstance(KEYSTORE_TYPE);
            ks.load(in, password);
            LOG.debug("Loaded Keystore file: " + CERT_PATH_ABS);
            return ks;
        }
        finally
        {
            IOUtils.closeQuietly(in);
        }
    }

    private String getPassword()
    {
        return PREFERENCES.get(USER_PASSWORD_KEY, null);
    }

    private void setPassword(String password)
    {
        PREFERENCES.put(USER_PASSWORD_KEY, password);
    }

    // the keystore for use by the Proxy  
    public KeyStore getKeyStore()
    {
        return sslKeyStore;
    }

    public String getKeyPassword()
    {
        return keyPassword;
    }

    public enum KeystoreMode
    {
        USER_KEYSTORE, // user-provided keystore
        JMETER_KEYSTORE, // keystore generated by JMeter; single entry
        DYNAMIC_KEYSTORE, // keystore generated by JMeter; dynamic entries
        NONE // cannot use keystore
    }

}
