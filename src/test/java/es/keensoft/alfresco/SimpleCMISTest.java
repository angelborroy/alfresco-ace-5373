package es.keensoft.alfresco;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.math.BigInteger;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.chemistry.opencmis.client.api.Document;
import org.apache.chemistry.opencmis.client.api.Folder;
import org.apache.chemistry.opencmis.client.api.Session;
import org.apache.chemistry.opencmis.client.api.SessionFactory;
import org.apache.chemistry.opencmis.client.runtime.SessionFactoryImpl;
import org.apache.chemistry.opencmis.commons.PropertyIds;
import org.apache.chemistry.opencmis.commons.SessionParameter;
import org.apache.chemistry.opencmis.commons.data.ContentStream;
import org.apache.chemistry.opencmis.commons.enums.BindingType;
import org.apache.chemistry.opencmis.commons.enums.VersioningState;
import org.apache.chemistry.opencmis.commons.impl.dataobjects.ContentStreamImpl;

public class SimpleCMISTest {
	
	private final static String userName="admin";
	private final static String userPass="admin";
	private final static String cmisUrl="http://localhost:8080/alfresco/api/-default-/public/cmis/versions/1.1/atom";
	
	public static void main(String... args) throws Exception {
		
		// CMIS session
		Map<String, String> parameter = new HashMap<String, String>();
		parameter.put(SessionParameter.USER, userName);
		parameter.put(SessionParameter.PASSWORD, userPass);
		parameter.put(SessionParameter.ATOMPUB_URL, cmisUrl);
		parameter.put(SessionParameter.BINDING_TYPE, BindingType.ATOMPUB.value());
		SessionFactory factory = SessionFactoryImpl.newInstance();
		Session session = factory.getRepositories(parameter).get(0).createSession();
		
		// Any document, just get it from everywhere
		Document sampleDocument = createDocument(session);
		
		// Apply an aspect including a protected property
		Map<String, Object> properties = new HashMap<String, Object>();
		List<Object> aspects = sampleDocument.getProperty(PropertyIds.SECONDARY_OBJECT_TYPE_IDS).getValues();
		aspects.add("P:cm:lockable");
		properties.put(PropertyIds.SECONDARY_OBJECT_TYPE_IDS, aspects);
		sampleDocument.updateProperties(properties);
		
		System.out.println("Success!");
		
	}
	
	private static Document createDocument(Session session) {
		
		Map<String, Object> properties = new HashMap<String, Object>();
		Folder folder = (Folder) session.getObjectByPath("/Compartido");
		
		String name = "myNewDocument-" + System.currentTimeMillis() + ".txt";

		properties.put(PropertyIds.OBJECT_TYPE_ID, "cmis:document");
		properties.put(PropertyIds.NAME, name);
		
		byte[] content = "Hello World!".getBytes();
		InputStream stream = new ByteArrayInputStream(content);
		ContentStream contentStream = new ContentStreamImpl(name, BigInteger.valueOf(content.length), "text/plain", stream);
		
		return folder.createDocument(properties, contentStream, VersioningState.MAJOR);
	}

}
