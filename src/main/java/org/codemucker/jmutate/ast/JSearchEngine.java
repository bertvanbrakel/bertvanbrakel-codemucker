package org.codemucker.jmutate.ast;

import java.io.Closeable;
import java.io.File;
import java.util.List;

import org.codemucker.jfind.Root;
import org.codemucker.jfind.RootVisitor;
import org.codemucker.lang.IBuilder;

import com.google.common.base.Preconditions;
import com.google.common.collect.Lists;
import com.orientechnologies.orient.core.Orient;
import com.orientechnologies.orient.core.config.OGlobalConfiguration;
import com.orientechnologies.orient.core.db.document.ODatabaseDocumentTx;
import com.orientechnologies.orient.core.intent.OIntentMassiveInsert;
import com.orientechnologies.orient.core.iterator.ORecordIteratorClass;
import com.orientechnologies.orient.core.record.impl.ODocument;

public class JSearchEngine implements Closeable {

	private final ODatabaseDocumentTx db;
	private final JAstParser parser;

	public static Builder with(){
		return new Builder();
	}
	
	private JSearchEngine(File dbDirectoy, List<Root> roots, JAstParser parser){
		Preconditions.checkNotNull(dbDirectoy,"expect indexing db directory");
		Preconditions.checkNotNull(roots,"expect roots");
		Preconditions.checkNotNull(parser, "expect parser");
		
		this.parser = parser;
        db = Orient.instance()
        	.getDatabaseFactory().createDatabase("graph","local:" + dbDirectoy.getAbsolutePath());

        //TODO:turn off locking while we index
        //TODO:put it in a reusable location so multiple calls find the already indexed code
        
        db.create();
        
        db.declareIntent(new OIntentMassiveInsert());
        OGlobalConfiguration.TX_USE_LOG.setValue(false);
        OGlobalConfiguration.CACHE_LEVEL1_ENABLED.setValue(false);
        OGlobalConfiguration.CACHE_LEVEL2_ENABLED.setValue(false);
        index(roots);
      
        //reset intent
        db.declareIntent(null);
        OGlobalConfiguration.TX_USE_LOG.setValue(true);
        OGlobalConfiguration.CACHE_LEVEL1_ENABLED.setValue(true);
        OGlobalConfiguration.CACHE_LEVEL2_ENABLED.setValue(true);
	}

	public void index(List<Root> roots) {
		RootVisitor indexer = new JSearchIndexingVisitor(db,parser);
		for(Root root:roots){
			root.accept(indexer);
		}
	}
	
	//TODO:add search query
	public void find(){
		ORecordIteratorClass<ODocument> types = db.browseClass("Type");
		System.out.println("JSearchEngine:browse DB");
		for (ODocument doc : types) {
			System.out.println("JSearchEngine:fqdn " + doc.field("fqdn"));
			System.out.println("     path:" + doc.field("resource"));
		}
		//db.query(ONativeQuery<OQueryContextNative>, iArgs)
		System.out.println("JSearchEngine:end browse DB");
	}
	
	public void findPatternNamedX(){
		
	}

	@Override
	public void close() {
        db.close();
	}
	
	public static class Builder {
		private File dbDirectory;
		private JAstParser parser;
		private List<Root> roots = Lists.newArrayList();
		
		public JSearchEngine build(){
			Preconditions.checkNotNull(dbDirectory, "expect a database drectory to be set (or use defaults)");
			Preconditions.checkNotNull(parser, "expect a parser to be set (or use defaults)");
			
			return new JSearchEngine(dbDirectory,roots,parser);
		}
		
		public Builder defaults(){
			useDefaultDBDirectory();
			useDefaultParser();
			return this;
		}

		public Builder useDefaultDBDirectory(){
			dbDirectory("c:/tmp/orientdb_codemucker_search_test" + System.currentTimeMillis());
			return this;
		}
		
		public Builder useDefaultParser(){
			parser(JAstParser.with().defaults().build());
			return this;
		}

		public Builder searchRoots(IBuilder<? extends Iterable<Root>> rootsBuilder){
			searchRoots(rootsBuilder.build());
			return this;
		}
		
		public Builder searchRoots(Iterable<Root> roots){
			for(Root r:roots){
				searchRoot(r);
			}
			return this;
		}
		
		public Builder searchRoot(Root root){
			this.roots.add(root);
			return this;
		}
		
		public Builder parser(IBuilder<JAstParser> builder){
			parser(builder.build());
			return this;
		}
		
		public Builder parser(JAstParser parser){
			this.parser = parser;
			return this;
		}
		
		public Builder dbDirectory(String dbDirectory){
			dbDirectory(new File(dbDirectory));
			return this;
		}
		
		public Builder dbDirectory(File dbDirectory){
			this.dbDirectory = dbDirectory;
			return this;
		}
	}
}
