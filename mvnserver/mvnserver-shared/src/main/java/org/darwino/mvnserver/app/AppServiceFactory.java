/*!COPYRIGHT HEADER! 
 *
 */

package org.darwino.mvnserver.app;

import java.util.List;

import org.darwino.mvnserver.repo.RepoService;

import com.darwino.commons.json.JsonArray;
import com.darwino.commons.json.JsonObject;
import com.darwino.commons.services.HttpService;
import com.darwino.commons.services.HttpServiceContext;
import com.darwino.commons.services.HttpServiceError;
import com.darwino.commons.services.rest.RestServiceBinder;
import com.darwino.commons.services.rest.RestServiceFactory;
import com.darwino.commons.util.Lic;
import com.darwino.commons.util.StringUtil;
import com.darwino.jsonstore.Database;
import com.darwino.jsonstore.Session;
import com.darwino.platform.DarwinoApplication;
import com.darwino.platform.DarwinoContext;
import com.darwino.platform.DarwinoHttpConstants;


/**
 * Application Service Factory.
 * 
 * This is the place where to define custom application services.
 * 
 * @author Philippe Riand
 */
public class AppServiceFactory extends RestServiceFactory {
	
	public class AppInformation extends HttpService {
		@Override
		public void service(HttpServiceContext context) {
			if(context.isGet()) {
				JsonObject o = new JsonObject();
				try {
					o.put("name", "darwinomavenserver"); //$NON-NLS-1$ //$NON-NLS-2$
					
					// Access to the app manifest
					AppManifest mf = (AppManifest)DarwinoApplication.get().getManifest();
					o.put("application", DarwinoApplication.get().toString() ); //$NON-NLS-1$
					o.put("label", mf.getLabel()); //$NON-NLS-1$
					o.put("description", mf.getDescription()); //$NON-NLS-1$
					
					// Access to the database session
					JsonObject jSession = new JsonObject();
					Session session = DarwinoContext.get().getSession();
					jSession.put("user", session.getUser().getDn()); //$NON-NLS-1$
					jSession.put("instanceId", session.getInstanceId()); //$NON-NLS-1$
					o.put("session", jSession); //$NON-NLS-1$
					
					// Add custom application information
					addAppInfo(context,o);
				} catch(Exception ex) {
					o.put("exception", HttpServiceError.exceptionAsJson(ex, false)); //$NON-NLS-1$
				}
				context.emitJson(o);
			} else {
				throw HttpServiceError.errorUnsupportedMethod(context.getMethod());
			}
		}
	}
	
	public class Properties extends HttpService {
		@Override
		public void service(HttpServiceContext context) {
			if(context.isGet()) {
				JsonObject o = new JsonObject();
				try {
					// Check if JSON query is supported by this DB driver
					o.put("jsonQuery", DarwinoApplication.get().getLocalJsonDBServer().isJsonQuerySupported()); //$NON-NLS-1$
										
					// Instances are only supported with the Enterprise edition
					o.put("useInstances", false); //$NON-NLS-1$
					if(Lic.isEnterpriseEdition()) {
						Session session = DarwinoContext.get().getSession();
						String dbName = DarwinoApplication.get().getManifest().getMainDatabase();
						Database db = session.getDatabase(dbName);
						if(db.isInstanceEnabled()) {
							o.put("useInstances", true); //$NON-NLS-1$
							// The instances can be fixed from a property or read from the database
							//JsonArray a = new JsonArray(session.getDatabaseInstances(dbName));
							JsonArray a = new JsonArray(AppDatabaseDef.getInstances());
							o.put("instances", a); //$NON-NLS-1$
						}
					}
					
					// Add custom properties
					addProperties(context,o);
				} catch(Exception ex) {
					o.put("exception", HttpServiceError.exceptionAsJson(ex, false)); //$NON-NLS-1$
				}
				context.emitJson(o);
			} else {
				throw HttpServiceError.errorUnsupportedMethod(context.getMethod());
			}
		}
	}
	
	public AppServiceFactory() {
		super(DarwinoHttpConstants.APPSERVICES_PATH);
	}
	
	protected void addAppInfo(HttpServiceContext context, JsonObject info) {
		// Add specific app information here..
	}
	
	protected void addProperties(HttpServiceContext context, JsonObject props) {
		// Add specific properties here...
	}
	
	@Override
	protected void createServicesBinders(List<RestServiceBinder> binders) {
		/////////////////////////////////////////////////////////////////////////////////
		// INFORMATION
		binders.add(new RestServiceBinder() {
			@Override
			public HttpService createService(HttpServiceContext context, String[] parts) {
				return new AppInformation();
			}
		});
		
		/////////////////////////////////////////////////////////////////////////////////
		// APPLICATION PROPERTIES
		binders.add(new RestServiceBinder("properties") { //$NON-NLS-1$
			@Override
			public HttpService createService(HttpServiceContext context, String[] parts) {
				return new Properties();
			}
		});
		
		// *******************************************************************************
		// * Repo service
		// *******************************************************************************
		binders.add(new RestServiceBinder("repo") { //$NON-NLS-1$
			@Override public HttpService createService(HttpServiceContext context, String[] parts) {
				return new RepoService(parts);
			}
			@Override public int match(String[] parts) {
				if(parts.length < 1) {
					return 0;
				} else if(StringUtil.equals(parts[0], getSignature()[0])) {
					// Match anything starting with "repo"
					return parts.length;
				} else {
					return 0;
				}
			}
		});
	}	
}
