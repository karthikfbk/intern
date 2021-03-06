package thesis.authz.federated_iot_rs;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.math.BigInteger;
import java.net.HttpURLConnection;
import java.net.InetSocketAddress;
import java.net.MalformedURLException;
import java.net.URL;
import java.security.KeyStore;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.cert.Certificate;
import java.security.spec.ECParameterSpec;
import java.security.interfaces.ECPublicKey;
import java.util.Base64;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.bouncycastle.asn1.ASN1ObjectIdentifier;
import org.bouncycastle.asn1.x9.ECNamedCurveTable;
import org.bouncycastle.asn1.x9.X9ECParameters;
import org.bouncycastle.crypto.params.ECNamedDomainParameters;
import org.bouncycastle.crypto.params.ECPublicKeyParameters;
import org.bouncycastle.math.ec.ECCurve;
import org.bouncycastle.math.ec.ECPoint;
import org.eclipse.californium.core.CoapResource;
import org.eclipse.californium.core.CoapServer;
import org.eclipse.californium.core.coap.CoAP;
import org.eclipse.californium.core.network.CoapEndpoint;
import org.eclipse.californium.core.network.config.NetworkConfig;
import org.eclipse.californium.core.server.resources.CoapExchange;
import org.eclipse.californium.core.server.resources.Resource;
import org.eclipse.californium.scandium.DTLSConnector;
import org.eclipse.californium.scandium.config.DtlsConnectorConfig;
import org.eclipse.californium.scandium.dtls.cipher.CipherSuite;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.upokecenter.cbor.CBORObject;

import COSE.AlgorithmID;
import COSE.ECPrivateKey;

import COSE.KeyKeys;
import COSE.MessageTag;
import COSE.OneKey;
import se.sics.ace.AceException;
import se.sics.ace.COSEparams;
import se.sics.ace.coap.rs.CoapAuthzInfo;
import se.sics.ace.coap.rs.dtlsProfile.DtlspPskStore;
import se.sics.ace.cwt.CwtCryptoCtx;
import se.sics.ace.examples.KissTime;
import se.sics.ace.examples.KissValidator;
import se.sics.ace.rs.AsInfo;
import se.sics.ace.rs.AuthzInfo;
import se.sics.ace.rs.TokenRepository;
import thesis.authz.federated_iot_core.AuthzInfo_ma;
import thesis.authz.federated_iot_core.CoapAuthzInfo_ma;
import thesis.authz.federated_iot_core.CoapDeliverer_ma;
import thesis.authz.federated_iot_core.CoapRSInfo;
import thesis.authz.federated_iot_core.CoapUpdate;
import thesis.authz.federated_iot_core.Coap_Discovery;
import thesis.authz.federated_iot_core.TokenRepository_ma;

/**
 * Hello world!
 *
 */
public class App 
{

	static byte[] key256 = {'a', 'b', 'c', 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16, 17, 18, 19, 20, 21, 22, 23, 24, 25, 26, 27,28, 29, 30, 31, 32};


	/**
	 * Definition of the Light_Kitchen Resource
	 */
	public static class Light_Kitchen extends CoapResource {

		private String geturl = "http://192.168.0.200/api/-hRQKg0CIXvcbMUmTPrULquRgkdpMiP71t9tFK6y/groups/0/";

		private String puturl = "http://192.168.0.200/api/-hRQKg0CIXvcbMUmTPrULquRgkdpMiP71t9tFK6y/groups/0/action";
		private int status = 0;
		/**
		 * Constructor
		 */
		public Light_Kitchen() {

			// set resource identifier
			super("light_kitchen");

			// set display name
			getAttributes().setTitle("Kitchen light");
		}

		@Override
		public void handleGET(CoapExchange exchange) {

			// respond to the request
			if(status == 0)
				exchange.respond("Light is OFF");
			if(status == 1)
				exchange.respond("Light is ON");

		}

		@Override
		public void handlePUT(CoapExchange exchange) {

			

			StringBuilder sb = new StringBuilder();  


			HttpURLConnection urlConnection=null;  
			try {  
				URL url = new URL(puturl);  
				urlConnection = (HttpURLConnection) url.openConnection();
				urlConnection.setDoOutput(true);   
				urlConnection.setRequestMethod("PUT");  
				urlConnection.setUseCaches(false);  
				urlConnection.setConnectTimeout(10000);  
				urlConnection.setReadTimeout(10000);  
				urlConnection.setRequestProperty("Content-Type","application/json");   

				urlConnection.connect();  

				//Create JSONObject here
				JSONObject jsonParam = new JSONObject();
				String payload = new String(exchange.getRequestPayload());
				System.out.println("Received payload " + payload);
				if(payload.equals("true")) {
					//Switch off the light
					jsonParam.put("on", true);
					jsonParam.put("hue", 54);
				}
				if(payload.equals("false")) {
					//Switch on the light
					jsonParam.put("on", false);
				}
				OutputStreamWriter out = new   OutputStreamWriter(urlConnection.getOutputStream());
				out.write(jsonParam.toString());
				out.close();  

				int HttpResult =urlConnection.getResponseCode();  
				if(HttpResult == HttpURLConnection.HTTP_OK){  
					BufferedReader br = new BufferedReader(new InputStreamReader(  
							urlConnection.getInputStream(),"utf-8"));  
					String line = null;  
					while ((line = br.readLine()) != null) {  
						sb.append(line + "\n");  
					}  
					br.close();  

					System.out.println(""+sb.toString());  
					if(payload.equals("true")) {
						exchange.respond("you just switched on the light");
						status = 1;
					}
					else if(payload.equals("false")) {
						exchange.respond("you just switched off the light");
						status = 0;
					}

				}else{  
					System.out.println(urlConnection.getResponseMessage());  
				}  
			} catch (MalformedURLException e) {  

				e.printStackTrace();  
			}  
			catch (IOException e) {  

				e.printStackTrace();  
			} catch (JSONException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}finally{  
				if(urlConnection!=null)  
					urlConnection.disconnect();  
			} 
		}
	}



	/**
	 * Definition of the Light_Living Resource
	 */
	public static class Light_Living extends CoapResource {

		/**
		 * Constructor
		 */
		public Light_Living() {

			// set resource identifier
			super("light_living");

			// set display name
			getAttributes().setTitle("Living room light");
		}

		@Override
		public void handleGET(CoapExchange exchange) {

			// respond to the request
			exchange.respond("Hi !! you just read the light in living room");
		}

		@Override
		public void handlePUT(CoapExchange exchange) {

			// respond to the request
			exchange.respond("Hi !! you just changed the light in living room");
		}
	}





	/**
	 * Definition of the Hello-World Resource
	 */
	public static class HelloWorldResource extends CoapResource {

		/**
		 * Constructor
		 */
		public HelloWorldResource() {

			// set resource identifier
			super("helloWorld");

			// set display name
			getAttributes().setTitle("Hello-World Resource");
		}

		@Override
		public void handleGET(CoapExchange exchange) {

			// respond to the request
			exchange.respond("Hello World!");
		}
	}

	/**
	 * Definition of the Temp Resource
	 */
	public static class TempResource extends CoapResource {

		/**
		 * Constructor
		 */
		public TempResource() {

			// set resource identifier
			super("temp");

			// set display name
			getAttributes().setTitle("Temp Resource");
		}

		@Override
		public void handleGET(CoapExchange exchange) {

			// respond to the request
			exchange.respond("19.0 C");
		}
	}

	private static TokenRepository_ma tr = null;

	private static AuthzInfo_ma ai = null;

	private static CoapServer rs = null;

	private static CoapDeliverer_ma dpd = null;


	public static void main( String[] args ) throws Exception
	{
		//int Port = Integer.parseInt(args[1]);
		int sPort = Integer.parseInt(args[1]);
		//int sPort2 = Integer.parseInt(args[2]);
		InputStream in = null;

		JSONObject rsconfig = readFile(args[0]);

		String keyStorelocation = rsconfig.getString("keystorelocation");
		String trustStorelocation = rsconfig.getString("truststorelocation");
		String keyStorepassword = rsconfig.getString("keystorepass");
		String trustStorepassword = rsconfig.getString("truststorepass");
		String alias = rsconfig.getString("alias");
		String rsname = rsconfig.getString("name");
		String tokenendpoint = rsconfig.getString("tokenendpoint");
		//String rs_infoendpoint = rsconfig.getString("rsinfoendpoint");
		String rootalias = rsconfig.getString("root");

		Map<String, Map<String, Set<String>>> myScopes = null;

		if(rsconfig.has("RESOURCES")) {
			//Read Resources
			JSONArray resources = rsconfig.getJSONArray("RESOURCES");

			myScopes = new HashMap<>();

			if(resources != null) {
				if(resources.length() > 0) {
					for(int i=0; i<resources.length();i++) {
						JSONObject resource = (JSONObject) resources.get(i);
						String name = resource.getString("name");
						String path = resource.getString("path");


						JSONArray rules = resource.getJSONArray("rules");

						for(Object orule:rules) {
							JSONObject rule = (JSONObject) orule;
							String r_scope = rule.getString("scope");
							JSONArray r_actions=rule.getJSONArray("actions");
							Set<String> actions = new HashSet<>();
							Map<String, Set<String>> myResource = new HashMap<>();
							for(Object o: r_actions) {
								actions.add(o.toString());
							}
							myResource.put(path, actions);
							myScopes.put(r_scope, myResource);
						}
					}


				}
			}
		}


		KissValidator valid = new KissValidator(Collections.singleton(rsname),
				myScopes);

		createTR(valid);
		tr = TokenRepository_ma.getInstance();


		String issuer_name = null;
		//This is the cwt that this rs shares with this issuer.
		//Using this it can verify the maced token by issuer.		
		CwtCryptoCtx cwt = null;
		String issuer_alias = null;
		if(rsconfig.has("ISSUER")) {
			//Read ISSUERS
			JSONObject issuer = (JSONObject) rsconfig.get("ISSUER");
			if(issuer != null) {

				issuer_name = issuer.getString("name");
				issuer_alias = issuer.getString("alias");
				JSONObject cose_param = (JSONObject) issuer.get("cose_param");
				MessageTag mt = null;
				AlgorithmID algid = null;
				AlgorithmID keyid = null;

				COSEparams coseP = null; 

				String messagetag = cose_param.getString("messagetag");
				switch(messagetag) {
				case "MAC0":
					String algorithmid = cose_param.getString("algorithmid");
					byte[] skey =  Base64.getDecoder().decode(cose_param.getString("skey"));
					switch(algorithmid) {
					case "HMAC_SHA_256":
						String keywrapid = cose_param.getString("keywrapid");
						switch(keywrapid) {
						case "Direct":

							coseP = new COSEparams(MessageTag.MAC0, 
									AlgorithmID.HMAC_SHA_256, AlgorithmID.Direct);

							break;
						default:
							break;
						}
						break;
					default:
						break;
					}

					cwt =  CwtCryptoCtx.mac0(skey, coseP.getAlg().AsCBOR());

					break;
				default:
					break;
				}
			}
		}

		// load the key store
		KeyStore keyStore = KeyStore.getInstance("JKS");			
		in = new FileInputStream(keyStorelocation);			
		keyStore.load(in, keyStorepassword.toCharArray());			
		in.close();


		// load the trust store
		KeyStore trustStore = KeyStore.getInstance("JKS");
		InputStream inTrust = new FileInputStream(trustStorelocation);
		trustStore.load(inTrust, trustStorepassword.toCharArray());

		Certificate ascert = trustStore.getCertificate(issuer_alias);
		OneKey pubkey = convertToOneKey_publickey(ascert.getPublicKey());
		//For asymmetric is always signature
		cwt =  CwtCryptoCtx.signVerify(pubkey, AlgorithmID.ECDSA_256.AsCBOR()); 


		//Set up the inner Authz-Info library
		ai = new AuthzInfo_ma(tr, Collections.singletonList(issuer_name), 
				new KissTime(), 
				null,
				valid, cwt);

		AsInfo asi 
		= new AsInfo("coaps://localhost/Token_ma/");
		Resource authzInfo = new CoapAuthzInfo_ma(ai);

		Resource l_l = new Light_Living();
		Resource l_k = new Light_Kitchen();

		StringBuilder tmp = new StringBuilder();
		for ( String key : myScopes.keySet() ) {
			tmp.append(key);
			tmp.append(" ");
		}


		//Resource rsinfo = new CoapRSInfo(rsname,issuer_name,tmp.toString().trim());


		rs = new CoapServer();
		rs.add(l_l);
		rs.add(l_k);
		rs.add(authzInfo);
		//rs.add(rsinfo);

		dpd = new CoapDeliverer_ma(rs.getRoot(), tr, null, asi); 



		// You can load multiple certificates if needed
		Certificate[] trustedCertificates = new Certificate[1];
		trustedCertificates[0] = trustStore.getCertificate(rootalias);

		PrivateKey key = (PrivateKey)keyStore.getKey(alias, keyStorepassword.toCharArray());
		java.security.cert.Certificate cert = keyStore.getCertificate(alias); 

		DtlsConnectorConfig.Builder config = new DtlsConnectorConfig.Builder(new InetSocketAddress(sPort));
		//config.setAddress(new InetSocketAddress(sPort));
		config.setSupportedCipherSuites(new CipherSuite[]{
				CipherSuite.TLS_ECDHE_ECDSA_WITH_AES_128_CCM_8});

		config.setIdentity((PrivateKey)keyStore.getKey(alias, keyStorepassword.toCharArray()),
				keyStore.getCertificateChain(alias), false);
		config.setTrustStore(trustedCertificates);
		config.setClientAuthenticationRequired(true);
		DTLSConnector connector = new DTLSConnector(config.build());
		rs.addEndpoint(
				new CoapEndpoint(connector, NetworkConfig.getStandard()));

		//Add another connecter without client authentication
		//because anyone can fetch /rsinfo endpoint information
		//		DtlsConnectorConfig.Builder config1 = new DtlsConnectorConfig.Builder(new InetSocketAddress(sPort2));
		//		config1.setSupportedCipherSuites(new CipherSuite[]{
		//				CipherSuite.TLS_ECDHE_ECDSA_WITH_AES_128_CCM_8});
		//
		//		config1.setIdentity((PrivateKey)keyStore.getKey(alias, keyStorepassword.toCharArray()),
		//				keyStore.getCertificateChain(alias), false);
		//		config1.setTrustStore(trustedCertificates);
		//		config1.setClientAuthenticationRequired(false);
		//		DTLSConnector connector1 = new DTLSConnector(config1.build());
		//
		//		rs.addEndpoint(
		//				new CoapEndpoint(connector1, NetworkConfig.getStandard()));

		//Add a CoAP (no 's') endpoint for authz-info
		//rs.addEndpoint(new CoapEndpoint(new InetSocketAddress(
		//		5683)));

		//Add discovery server for coap discovery
		Coap_Discovery dis = new Coap_Discovery(rsname,issuer_name , tmp.toString().trim(), tokenendpoint);
		dis.start();
		System.out.println("My Scopes " + tmp.toString().trim());

		Resource update = new CoapUpdate(rsname,issuer_name,rs,rootalias,alias,
				keyStore,keyStorepassword,trustStore,trustStorepassword,trustedCertificates,sPort);

		rs.add(update);
		rs.setMessageDeliverer(dpd);
		rs.start();
		System.out.println("Server starting");
	}

	/**
	 * @param valid 
	 * @throws IOException 
	 * 
	 */
	private static void createTR(KissValidator valid) throws IOException {
		try {
			TokenRepository_ma.create(valid, "tokens.json", null);
		} catch (AceException e) {
			System.err.println(e.getMessage());
			try {
				TokenRepository tr = TokenRepository.getInstance();
				tr.close();
				new File("tokens.json").delete();
				TokenRepository.create(valid, "tokens.json", null);
			} catch (AceException e2) {
				throw new RuntimeException(e2);
			}


		}
	}

	private static OneKey convertToOneKey_publickey(PublicKey pubkey) {
		ECPublicKey mypub = (ECPublicKey) pubkey;

		byte[] X = mypub.getW().getAffineX().toByteArray();		

		byte[] Y = mypub.getW().getAffineY().toByteArray();		

		// assumes that x and y are (unsigned) big endian encoded
		BigInteger xbi = new BigInteger(1, X);
		BigInteger ybi = new BigInteger(1, Y);
		X9ECParameters x9 = ECNamedCurveTable.getByName("secp256r1");
		ASN1ObjectIdentifier oid = ECNamedCurveTable.getOID("secp256r1");
		ECCurve curve = x9.getCurve();
		ECPoint point = curve.createPoint(xbi, ybi);
		ECNamedDomainParameters dParams = new ECNamedDomainParameters(oid,
				x9.getCurve(), x9.getG(), x9.getN(), x9.getH(), x9.getSeed());
		ECPublicKeyParameters pubKey = new ECPublicKeyParameters(point, dParams);
		System.out.println(pubKey);

		byte[] rgbX = pubKey.getQ().normalize().getXCoord().getEncoded();
		byte[] rgbY = pubKey.getQ().normalize().getYCoord().getEncoded();	

		OneKey key1 = new OneKey();
		key1.add(KeyKeys.KeyType, KeyKeys.KeyType_EC2);
		key1.add(KeyKeys.EC2_Curve, KeyKeys.EC2_P256);
		key1.add(KeyKeys.EC2_X, CBORObject.FromObject(rgbX));
		key1.add(KeyKeys.EC2_Y, CBORObject.FromObject(rgbY));


		return key1;
	}


	/**
	 * Stops the server
	 * 
	 * @throws IOException 
	 * @throws AceException 
	 */
	public static void stop() throws IOException, AceException {
		rs.stop();
		dpd.close();
		ai.close();
		tr.close();
		new File("tokens.json").delete();
	}

	static JSONObject readFile(String Filename) {

		String result = "";

		try {
			BufferedReader br = new BufferedReader(new FileReader(Filename));
			StringBuilder sb = new StringBuilder();
			String line = br.readLine();
			while(line != null) {
				sb.append(line);
				line=br.readLine();
			}
			result = sb.toString();
			br.close();
		} catch(Exception e) {
			e.printStackTrace();
		}
		return new JSONObject(result);
	}

}
