package com.github.mongobee.dao;

import static com.github.mongobee.changeset.ChangeEntry.CHANGELOG_COLLECTION;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.bson.Document;
import org.junit.Test;
import org.mockito.Mockito;

import com.github.fakemongo.Fongo;
import com.github.mongobee.exception.MongobeeConfigurationException;
import com.mongodb.FongoMongoCollection;
import com.mongodb.MongoClient;
import com.mongodb.client.MongoDatabase;

/**
 * @author lstolowski
 * @since 10.12.14
 */
public class ChangeEntryDaoTest {
  private static final String TEST_SERVER = "testServer";
  private static final String DB_NAME = "mongobeetest";

  @Test
  public void shouldCreateChangeIdAuthorIndexIfNotFound() throws MongobeeConfigurationException {

    // given
    ChangeEntryDao dao = new ChangeEntryDao();

    MongoClient mongoClient = mock(MongoClient.class);
    MongoDatabase db = new Fongo(TEST_SERVER).getDatabase(DB_NAME);

    when(mongoClient.getDatabase(anyString())).thenReturn(db);

    ChangeEntryIndexDao indexDaoMock = mock(ChangeEntryIndexDao.class);
    when(indexDaoMock.findRequiredChangeAndAuthorIndex(db)).thenReturn(null);
    dao.setIndexDao(indexDaoMock);

    // when
    dao.connectMongoDb(mongoClient, DB_NAME);

    //then
    verify(indexDaoMock, times(1)).createRequiredUniqueIndex(any(FongoMongoCollection.class));
    // and not
    verify(indexDaoMock, times(0)).dropIndex(any(FongoMongoCollection.class), any(Document.class));
  }

  @Test
  public void shouldNotCreateChangeIdAuthorIndexIfFound() throws MongobeeConfigurationException {

    // given
    MongoClient mongoClient = mock(MongoClient.class);
    MongoDatabase db = new Fongo(TEST_SERVER).getDatabase(DB_NAME);
    when(mongoClient.getDatabase(anyString())).thenReturn(db);

    ChangeEntryDao dao = new ChangeEntryDao();
    ChangeEntryIndexDao indexDaoMock = mock(ChangeEntryIndexDao.class);
    when(indexDaoMock.findRequiredChangeAndAuthorIndex(db)).thenReturn(new Document());
    when(indexDaoMock.isUnique(any(Document.class))).thenReturn(true);
    dao.setIndexDao(indexDaoMock);

    // when
    dao.connectMongoDb(mongoClient, DB_NAME);

    //then
    verify(indexDaoMock, times(0)).createRequiredUniqueIndex(db.getCollection(CHANGELOG_COLLECTION));
    // and not
    verify(indexDaoMock, times(0)).dropIndex(db.getCollection(CHANGELOG_COLLECTION), new Document());
  }

  @Test
  public void shouldRecreateChangeIdAuthorIndexIfFoundNotUnique() throws MongobeeConfigurationException {

    // given
    MongoClient mongoClient = mock(MongoClient.class);
    MongoDatabase db = new Fongo(TEST_SERVER).getDatabase(DB_NAME);
    when(mongoClient.getDatabase(anyString())).thenReturn(db);

    ChangeEntryDao dao = new ChangeEntryDao();
    ChangeEntryIndexDao indexDaoMock = mock(ChangeEntryIndexDao.class);
    when(indexDaoMock.findRequiredChangeAndAuthorIndex(db)).thenReturn(new Document());
    when(indexDaoMock.isUnique(any(Document.class))).thenReturn(false);
    dao.setIndexDao(indexDaoMock);

    // when
    dao.connectMongoDb(mongoClient, DB_NAME);

    //then
    verify(indexDaoMock, times(1)).dropIndex(any(FongoMongoCollection.class), any(Document.class));
    // and
    verify(indexDaoMock, times(1)).createRequiredUniqueIndex(any(FongoMongoCollection.class));
  }

  @Test
  public void shouldInitiateLock() throws MongobeeConfigurationException {

    // given
    MongoClient mongoClient = mock(MongoClient.class);
    MongoDatabase db = new Fongo(TEST_SERVER).getDatabase(DB_NAME);
    when(mongoClient.getDatabase(anyString())).thenReturn(db);

    ChangeEntryDao dao = new ChangeEntryDao();
    ChangeEntryIndexDao indexDaoMock = mock(ChangeEntryIndexDao.class);
    dao.setIndexDao(indexDaoMock);

    LockDao lockDao = mock(LockDao.class);
    dao.setLockDao(lockDao);

    // when
    dao.connectMongoDb(mongoClient, DB_NAME);

    // then
    verify(lockDao).intitializeLock(db);

  }

  @Test
  public void shouldGetLockWhenLockDaoGetsLock() throws Exception {

    // given
    MongoClient mongoClient = mock(MongoClient.class);
    MongoDatabase db = new Fongo(TEST_SERVER).getDatabase(DB_NAME);
    when(mongoClient.getDatabase(anyString())).thenReturn(db);

    ChangeEntryDao dao = new ChangeEntryDao();

    LockDao lockDao = mock(LockDao.class);
    when(lockDao.acquireLock(any(MongoDatabase.class))).thenReturn(true);
    dao.setLockDao(lockDao);

    dao.connectMongoDb(mongoClient, DB_NAME);

    // when
    boolean hasLock = dao.acquireProcessLock();

    // then
    assertTrue(hasLock);
  }

  @Test
  public void shouldReleaseLockFromLockDao() throws Exception {

    // given
    MongoClient mongoClient = mock(MongoClient.class);
    MongoDatabase db = new Fongo(TEST_SERVER).getDatabase(DB_NAME);
    when(mongoClient.getDatabase(anyString())).thenReturn(db);

    ChangeEntryDao dao = new ChangeEntryDao();

    LockDao lockDao = mock(LockDao.class);
    dao.setLockDao(lockDao);

    dao.connectMongoDb(mongoClient, DB_NAME);

    // when
    dao.releaseProcessLock();

    // then
    verify(lockDao).releaseLock(any(MongoDatabase.class));
  }

  @Test
  public void shouldCheckLockHeldFromFromLockDao() throws Exception {

    // given
    MongoClient mongoClient = mock(MongoClient.class);
    MongoDatabase db = new Fongo(TEST_SERVER).getDatabase(DB_NAME);
    when(mongoClient.getDatabase(anyString())).thenReturn(db);

    ChangeEntryDao dao = new ChangeEntryDao();

    LockDao lockDao = mock(LockDao.class);
    dao.setLockDao(lockDao);

    dao.connectMongoDb(mongoClient, DB_NAME);

    // when
    when(lockDao.isLockHeld(db)).thenReturn(true);

    boolean lockHeld = dao.isProccessLockHeld();

    // then
    assertTrue(lockHeld);
  }

}
