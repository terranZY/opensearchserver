/**   
 * License Agreement for OpenSearchServer
 *
 * Copyright (C) 2008-2013 Emmanuel Keller / Jaeksoft
 * 
 * http://www.open-search-server.com
 * 
 * This file is part of OpenSearchServer.
 *
 * OpenSearchServer is free software: you can redistribute it and/or
 * modify it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 * OpenSearchServer is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with OpenSearchServer. 
 *  If not, see <http://www.gnu.org/licenses/>.
 **/

package com.jaeksoft.searchlib.request;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import javax.xml.xpath.XPathExpressionException;

import org.apache.lucene.queryParser.QueryParser;
import org.apache.lucene.search.BooleanClause.Occur;
import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.Query;
import org.w3c.dom.DOMException;
import org.w3c.dom.Node;
import org.xml.sax.SAXException;

import com.jaeksoft.searchlib.SearchLibException;
import com.jaeksoft.searchlib.config.Config;
import com.jaeksoft.searchlib.function.expression.SyntaxError;
import com.jaeksoft.searchlib.query.ParseException;
import com.jaeksoft.searchlib.snippet.SnippetFieldList;
import com.jaeksoft.searchlib.util.DomUtils;
import com.jaeksoft.searchlib.util.XPathParser;
import com.jaeksoft.searchlib.util.XmlWriter;

public class SearchFieldRequest extends AbstractSearchRequest implements
		RequestInterfaces.ReturnedFieldInterface,
		RequestInterfaces.FilterListInterface {

	public final static String SEARCHFIELD_QUERY_NODE_NAME = "query";
	public final static String SEARCHFIELD_SYNONYMS_NODE_NAME = "synonyms";

	private List<SearchField> searchFields;

	private Set<String> synonymsSet;

	public SearchFieldRequest() {
		super(null, RequestTypeEnum.SearchFieldRequest);
	}

	public SearchFieldRequest(Config config) {
		super(config, RequestTypeEnum.SearchFieldRequest);
	}

	@Override
	protected void setDefaultValues() {
		super.setDefaultValues();
		searchFields = new ArrayList<SearchField>(0);
		synonymsSet = new TreeSet<String>();
	}

	@Override
	public void copyFrom(AbstractRequest request) {
		super.copyFrom(request);
		SearchFieldRequest searchFieldRequest = (SearchFieldRequest) request;
		this.searchFields = new ArrayList<SearchField>(0);
		if (searchFieldRequest.searchFields != null)
			for (SearchField searchField : searchFieldRequest.searchFields)
				this.searchFields.add(searchField.clone());
		synonymsSet.clear();
		synonymsSet.addAll(searchFieldRequest.synonymsSet);
	}

	@Override
	protected Query newSnippetQuery(String queryString) throws IOException,
			ParseException, SyntaxError, SearchLibException {
		BooleanQuery complexQuery = new BooleanQuery();
		SnippetFieldList snippetFieldList = getSnippetFieldList();
		Occur occur = defaultOperator == QueryParser.Operator.AND ? Occur.MUST
				: Occur.SHOULD;
		for (SearchField searchField : searchFields)
			if (snippetFieldList.get(searchField.getField()) != null)
				searchField.addQuery(analyzer, queryString, complexQuery,
						phraseSlop, occur);
		return complexQuery;
	}

	@Override
	public Query newComplexQuery(String queryString) throws ParseException,
			SyntaxError, SearchLibException, IOException {
		Occur occur = defaultOperator == QueryParser.Operator.AND ? Occur.MUST
				: Occur.SHOULD;
		BooleanQuery complexQuery = new BooleanQuery();
		for (SearchField searchField : searchFields)
			searchField.addQuery(analyzer, queryString, complexQuery,
					phraseSlop, occur);
		return complexQuery;
	}

	@Override
	public void fromXmlConfigNoLock(Config config, XPathParser xpp,
			Node requestNode) throws XPathExpressionException, DOMException,
			ParseException, InstantiationException, IllegalAccessException,
			ClassNotFoundException {
		super.fromXmlConfigNoLock(config, xpp, requestNode);
		List<Node> fieldNodeList = DomUtils.getNodes(requestNode,
				SEARCHFIELD_QUERY_NODE_NAME, SearchField.SEARCHFIELD_NODE_NAME);
		if (fieldNodeList != null)
			for (Node fieldNode : fieldNodeList)
				searchFields.add(new SearchField(fieldNode));
		List<Node> synonymsNodeList = DomUtils.getNodes(requestNode,
				SEARCHFIELD_QUERY_NODE_NAME, SEARCHFIELD_SYNONYMS_NODE_NAME);
		if (synonymsNodeList != null)
			for (Node synonymsNode : synonymsNodeList)
				addSynonyms(synonymsNode.getTextContent());
	}

	@Override
	public void writeSubXmlConfig(XmlWriter xmlWriter) throws SAXException {
		xmlWriter.startElement(SEARCHFIELD_QUERY_NODE_NAME);
		for (SearchField searchField : searchFields)
			searchField.writeXmlConfig(xmlWriter);
		for (String synonyms : synonymsSet) {
			xmlWriter.startElement(SEARCHFIELD_SYNONYMS_NODE_NAME);
			xmlWriter.textNode(synonyms);
			xmlWriter.endElement();
		}
		xmlWriter.endElement();
	}

	@Override
	public String getInfo() {
		rwl.r.lock();
		try {
			return searchFields.toString();
		} finally {
			rwl.r.unlock();
		}
	}

	public Collection<SearchField> getSearchFields() {
		rwl.r.lock();
		try {
			return searchFields;
		} finally {
			rwl.r.unlock();
		}
	}

	public void add(SearchField searchField) {
		rwl.w.lock();
		try {
			searchFields.add(searchField);
			resetNoLock();
		} finally {
			rwl.w.unlock();
		}
	}

	public void remove(SearchField searchField) {
		rwl.w.lock();
		try {
			searchFields.remove(searchField);
			resetNoLock();
		} finally {
			rwl.w.unlock();
		}
	}

	public void addSynonyms(String synonyms) {
		rwl.w.lock();
		try {
			synonymsSet.add(synonyms);
			resetNoLock();
		} finally {
			rwl.w.unlock();
		}
	}

	public void removeSynonyms(String synonyms) {
		rwl.w.lock();
		try {
			synonymsSet.remove(synonyms);
			resetNoLock();
		} finally {
			rwl.w.unlock();
		}
	}

	public Collection<String> getSynonyms() {
		rwl.r.lock();
		try {
			return synonymsSet;
		} finally {
			rwl.r.unlock();
		}
	}

}
