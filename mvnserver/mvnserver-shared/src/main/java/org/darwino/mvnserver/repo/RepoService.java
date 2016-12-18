package org.darwino.mvnserver.repo;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Arrays;

import org.darwino.mvnserver.app.AppManifest;

import com.darwino.commons.json.JsonException;
import com.darwino.commons.services.HttpService;
import com.darwino.commons.services.HttpServiceContext;
import com.darwino.commons.services.HttpServiceError;
import com.darwino.commons.util.StringUtil;
import com.darwino.commons.util.io.Content;
import com.darwino.commons.util.io.StreamUtil;
import com.darwino.commons.util.io.content.ByteBufferContent;
import com.darwino.jsonstore.Attachment;
import com.darwino.jsonstore.Database;
import com.darwino.jsonstore.Document;
import com.darwino.jsonstore.Store;

public class RepoService extends HttpService {
	
	private static final String ATT_NAME = "content"; //$NON-NLS-1$
	
	private final String[] parts;
	
	public RepoService(String[] parts) {
		this.parts = parts;
	}

	@Override
	public void checkAuthorized(HttpServiceContext context) {
		super.checkAuthorized(context);
	}
	
	@Override
	public void service(HttpServiceContext context) {
		try {
			if(context.isGet()) {
				doGet(context);
			} else if(context.isPut()) {
				doPut(context);
			} else {
				System.out.println("Received unexpected method: " + context.getMethod());
				throw HttpServiceError.errorUnsupportedMethod(context.getMethod());
			}
		} catch(Exception e) {
			try {
				context.emitException(e);
			} catch (IOException e1) {
				e1.printStackTrace();
				throw HttpServiceError.error500(e1);
			}
		}
	}

	private void doGet(HttpServiceContext context) throws JsonException, IOException {
		System.out.println("Received GET for " + Arrays.asList(parts));
		
		Document doc = findExistingDocument(getFilePath());
		if(doc != null) {
			System.out.println("Found doc!");
			Attachment att = doc.getAttachment(ATT_NAME);
			if(att != null) {
				String fileName = parts[parts.length-1];
				context.setResponseHeader("Content-Disposition", "attachment; filename=\"" + fileName + "\""); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
				@SuppressWarnings("resource")
				OutputStream os = context.getOutputStream();
				@SuppressWarnings("resource")
				InputStream is = att.getInputStream();
				try {
					StreamUtil.copyStream(is, os);
				} finally {
					StreamUtil.close(is);
				}
			} else {
				throw HttpServiceError.errorNotFound();
			}
		} else {
			throw HttpServiceError.errorNotFound();
		}
	}
	
	private void doPut(HttpServiceContext context) throws IOException, JsonException {
		System.out.println("Received PUT for " + Arrays.asList(parts));
		
		storeDocument(getFilePath(), context.getContentType(), context.getInputStream());
		
		context.emitText("success");
	}
	
	private String getFilePath() {
		return StringUtil.join(Arrays.asList(parts).subList(1, parts.length), "/"); //$NON-NLS-1$
	}
	
	// *******************************************************************************
	// * DB operations
	// *******************************************************************************
	
	private Document findExistingDocument(String path) throws JsonException {
		Database db = AppManifest.getDatabase();
		Store store = db.getStore(Database.STORE_DEFAULT);
		if(store.documentExists(path)) {
			return store.loadDocument(path);
		} else {
			return null;
		}
	}
	private Document findOrCreateDocument(String path) throws JsonException {
		Document existing = findExistingDocument(path);
		if(existing != null) {
			return existing;
		} else {
			Database db = AppManifest.getDatabase();
			Store store = db.getStore(Database.STORE_DEFAULT);
			return store.newDocument(path);
		}
	}
	
	private void storeDocument(String path, String contentType, InputStream is) throws JsonException, IOException {
		Document doc = findOrCreateDocument(path);
		doc.deleteAllAttachments();
		@SuppressWarnings("resource")
		Content content = new ByteBufferContent(is, contentType);
		try {
			doc.createAttachment(ATT_NAME, content);
		} finally {
			StreamUtil.close(content);
		}
		doc.save();
	}
}
