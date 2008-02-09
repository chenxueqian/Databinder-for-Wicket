/*
 * Databinder: a simple bridge from Wicket to Hibernate
 * Copyright (C) 2006  Nathan Hamblen nathan@technically.us
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 * 
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 * 
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA  02110-1301  USA
 */

/*
 * Note: this class contains code adapted from wicket-contrib-database. 
 */

package net.databinder.hib;

import java.util.HashSet;

import net.databinder.CookieRequestCycle;

import org.apache.wicket.Page;
import org.apache.wicket.Response;
import org.apache.wicket.protocol.http.WebApplication;
import org.apache.wicket.protocol.http.WebRequest;
import org.hibernate.Session;
import org.hibernate.context.ManagedSessionContext;

/**
 * <p>Opens Hibernate sessions and transactions as required and closes them at a request's
 * end. Uncomitted transactions are rolled back. Uses keyed Hibernate session factories from
 * DataStaticService.</p>
 * @see DataStaticService
 * @author Nathan Hamblen
 */
public class DataRequestCycle extends CookieRequestCycle implements HibernateRequestCycle {
	
	/** Keys for session factories that have been opened for this request */ 
	protected HashSet<Object> keys = new HashSet<Object>();

	public DataRequestCycle(WebApplication application, WebRequest request, Response response) {
		super(application, request, response);
	}

	/** Roll back active transactions and close session. */
	protected void closeSession(Object key) {
		Session sess = Databinder.getHibernateSession(key);
		
		if (sess.isOpen())
			try {
				if (sess.getTransaction().isActive())
					sess.getTransaction().rollback();
			} finally {
				sess.close();
			}
	}

	/**
	 * Called by DataStaticService when a session is needed and does not already exist. 
	 * Opens a new thread-bound Hibernate session.
	 */
	public void dataSessionRequested(Object key) {
		openHibernateSession(key);
	}
	
	/**
	 * Open a session and begin a transaction for the keyed session factory.
	 * @param key object, or null for the default factory
	 * @return newly opened session
	 */
	protected org.hibernate.classic.Session openHibernateSession(Object key) {
		org.hibernate.classic.Session sess = Databinder.getHibernateSessionFactory(key).openSession();
		sess.beginTransaction();
		ManagedSessionContext.bind(sess);
		keys.add(key);
		return sess;
	}

	/**
	 * Closes all Hibernate sessions opened for this request. If a transaction has
	 * not been committed, it will be rolled back before closing the session.
	 * @see net.databinder.components.hibernate.DataForm#onSubmit()
	 */
	@Override
	protected void onEndRequest() {
		for (Object key : keys) {
			if (!ManagedSessionContext.hasBind(Databinder.getHibernateSessionFactory(key)))
				return;
			closeSession(key);
			ManagedSessionContext.unbind(Databinder.getHibernateSessionFactory(key));
		}
	}

	/** 
	 * Closes and reopens sessions for this request cycle. Unrelated models may try to load 
	 * themselves after this point. 
	 */
	@Override
	public Page onRuntimeException(Page page, RuntimeException e) {
		onEndRequest();
		onBeginRequest();
		return null;
	}

}
