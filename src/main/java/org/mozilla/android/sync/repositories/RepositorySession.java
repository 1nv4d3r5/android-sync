/* ***** BEGIN LICENSE BLOCK *****
 * Version: MPL 1.1/GPL 2.0/LGPL 2.1
 *
 * The contents of this file are subject to the Mozilla Public License Version
 * 1.1 (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 * http://www.mozilla.org/MPL/
 *
 * Software distributed under the License is distributed on an "AS IS" basis,
 * WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License
 * for the specific language governing rights and limitations under the
 * License.
 *
 * The Original Code is Android Sync Client.
 *
 * The Initial Developer of the Original Code is
 * the Mozilla Foundation.
 * Portions created by the Initial Developer are Copyright (C) 2011
 * the Initial Developer. All Rights Reserved.
 *
 * Contributor(s):
 * Jason Voll <jvoll@mozilla.com>
 * Richard Newman <rnewman@mozilla.com>
 *
 * Alternatively, the contents of this file may be used under the terms of
 * either the GNU General Public License Version 2 or later (the "GPL"), or
 * the GNU Lesser General Public License Version 2.1 or later (the "LGPL"),
 * in which case the provisions of the GPL or the LGPL are applicable instead
 * of those above. If you wish to allow use of your version of this file only
 * under the terms of either the GPL or the LGPL, and not to allow others to
 * use your version of this file under the terms of the MPL, indicate your
 * decision by deleting the provisions above and replace them with the notice
 * and other provisions required by the GPL or the LGPL. If you do not delete
 * the provisions above, a recipient may use your version of this file under
 * the terms of any one of the MPL, the GPL or the LGPL.
 *
 * ***** END LICENSE BLOCK ***** */

package org.mozilla.android.sync.repositories;

import org.mozilla.android.sync.repositories.delegates.RepositorySessionBeginDelegate;
import org.mozilla.android.sync.repositories.delegates.RepositorySessionFetchRecordsDelegate;
import org.mozilla.android.sync.repositories.delegates.RepositorySessionFinishDelegate;
import org.mozilla.android.sync.repositories.delegates.RepositorySessionGuidsSinceDelegate;
import org.mozilla.android.sync.repositories.delegates.RepositorySessionStoreDelegate;
import org.mozilla.android.sync.repositories.delegates.RepositorySessionWipeDelegate;
import org.mozilla.android.sync.repositories.domain.Record;

import android.util.Log;

/**
 * A RepositorySession is created and used thusly:
 *
 * * Construct, with a reference to its parent Repository, by calling
 *   Repository.createSession().
 * * Populate with saved information by calling unbundle().
 * * Begin a sync by calling begin().
 * * Perform operations such as fetchSince() and store().
 * * Finish by calling finish(), retrieving and storing the current bundle.
 *
 * @author rnewman
 *
 */
public abstract class RepositorySession {
  
  public enum SessionStatus {
    UNSTARTED,
    ACTIVE,
    DONE
  }

  private static final String tag = "RepositorySession";
  protected SessionStatus status = SessionStatus.UNSTARTED;
  protected Repository repository;

  // The time that the last sync on this collection completed, in milliseconds.
  protected long lastSyncTimestamp;
  protected long syncBeginTimestamp;

  public RepositorySession(Repository repository) {
    this.repository = repository;
  }

  public abstract void guidsSince(long timestamp, RepositorySessionGuidsSinceDelegate delegate);
  public abstract void fetchSince(long timestamp, RepositorySessionFetchRecordsDelegate delegate);
  public abstract void fetch(String[] guids, RepositorySessionFetchRecordsDelegate delegate);
  public abstract void fetchAll(RepositorySessionFetchRecordsDelegate delegate);
  public abstract void store(Record record, RepositorySessionStoreDelegate delegate);
  public abstract void wipe(RepositorySessionWipeDelegate delegate);

  public void unbundle(RepositorySessionBundle bundle) {
    this.lastSyncTimestamp = 0;
    if (bundle == null) {
      return;
    }
    if (bundle.containsKey("timestamp")) {
      try {
        this.lastSyncTimestamp = bundle.getLong("timestamp");
      } catch (Exception e) {
        // Defaults to 0 above.
      }
    }
  }

  public void begin(RepositorySessionBeginDelegate delegate) {
     if (this.status == SessionStatus.UNSTARTED) {
      this.status = SessionStatus.ACTIVE;
      this.syncBeginTimestamp = System.currentTimeMillis();
      delegate.onBeginSucceeded(this);
    } else {
      Log.e(tag, "Tried to begin() an already active or finished session");
      delegate.onBeginFailed(new InvalidSessionTransitionException(null));
    }
  }

  protected RepositorySessionBundle getBundle() {
    return this.getBundle(null);
  }

  /**
   * Override this in your subclasses to return values to save between sessions.
   * @param optional
   * @return
   */
  protected RepositorySessionBundle getBundle(RepositorySessionBundle optional) {
    RepositorySessionBundle bundle = (optional == null) ? new RepositorySessionBundle() : optional;

    // TODO: real value.
    bundle.put("timestamp", this.lastSyncTimestamp);
    return bundle;
  }

  public void finish(RepositorySessionFinishDelegate delegate) {
    if (this.status == SessionStatus.ACTIVE) {
      this.status = SessionStatus.DONE;
      delegate.onFinishSucceeded(this, this.getBundle(null));
    } else {
      Log.e(tag, "Tried to finish() an unstarted or already finished session");
      delegate.onFinishFailed(new InvalidSessionTransitionException(null));
    }
  }
 
  protected boolean confirmSessionActive() {
    return status == SessionStatus.ACTIVE ? true : false;
  }

  public void abort() {
    // TODO: do something here.

  }
}
