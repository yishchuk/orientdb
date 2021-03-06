/*
 *
 *  * Copyright 2010-2016 OrientDB LTD (http://orientdb.com)
 *  *
 *  * Licensed under the Apache License, Version 2.0 (the "License");
 *  * you may not use this file except in compliance with the License.
 *  * You may obtain a copy of the License at
 *  *
 *  *      http://www.apache.org/licenses/LICENSE-2.0
 *  *
 *  * Unless required by applicable law or agreed to in writing, software
 *  * distributed under the License is distributed on an "AS IS" BASIS,
 *  * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  * See the License for the specific language governing permissions and
 *  * limitations under the License.
 *  
 */

package com.orientechnologies.lucene.test;

import com.orientechnologies.orient.core.command.script.OCommandScript;
import com.orientechnologies.orient.core.record.impl.ODocument;
import com.orientechnologies.orient.core.sql.OCommandSQL;
import com.orientechnologies.orient.core.sql.query.OSQLSynchQuery;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.io.InputStream;
import java.util.List;

/**
 * Created by enricorisa on 08/10/14.
 */
public class LuceneContextTest extends BaseLuceneTest {

  @Before
  public void init() {
    InputStream stream = ClassLoader.getSystemResourceAsStream("testLuceneIndex.sql");

    db.command(new OCommandScript("sql", getScriptFromStream(stream))).execute();

    db.command(new OCommandSQL("create index Song.title on Song (title) FULLTEXT ENGINE LUCENE")).execute();
    db.command(new OCommandSQL("create index Song.author on Song (author) FULLTEXT ENGINE LUCENE")).execute();

  }

  @Test
  public void testContext() {

    List<ODocument> docs = db
        .query(new OSQLSynchQuery<ODocument>("select *,$score from Song where [title] LUCENE \"(title:man)\""));

    Assert.assertEquals(docs.size(), 14);

    Float latestScore = 100f;
    for (ODocument doc : docs) {
      Float score = doc.field("$score");
      Assert.assertNotNull(score);
      Assert.assertTrue(score <= latestScore);
      latestScore = score;
    }

    docs = db.query(new OSQLSynchQuery<ODocument>(
        "select *,$totalHits,$Song_title_totalHits from Song where [title] LUCENE \"(title:man)\" limit 1"));
    Assert.assertEquals(docs.size(), 1);

    ODocument doc = docs.iterator().next();
    Assert.assertEquals(14, doc.<Object>field("$totalHits"));
    Assert.assertEquals(14, doc.<Object>field("$Song_title_totalHits"));

  }

}
