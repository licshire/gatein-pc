/******************************************************************************
 * JBoss, a division of Red Hat                                               *
 * Copyright 2006, Red Hat Middleware, LLC, and individual                    *
 * contributors as indicated by the @authors tag. See the                     *
 * copyright.txt in the distribution for a full listing of                    *
 * individual contributors.                                                   *
 *                                                                            *
 * This is free software; you can redistribute it and/or modify it            *
 * under the terms of the GNU Lesser General Public License as                *
 * published by the Free Software Foundation; either version 2.1 of           *
 * the License, or (at your option) any later version.                        *
 *                                                                            *
 * This software is distributed in the hope that it will be useful,           *
 * but WITHOUT ANY WARRANTY; without even the implied warranty of             *
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU           *
 * Lesser General Public License for more details.                            *
 *                                                                            *
 * You should have received a copy of the GNU Lesser General Public           *
 * License along with this software; if not, write to the Free                *
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA         *
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.                   *
 ******************************************************************************/
package org.gatein.pc.state;

import org.gatein.pc.api.Portlet;
import org.gatein.pc.api.PortletContext;
import org.gatein.pc.api.PortletInvokerException;
import org.gatein.pc.api.state.PropertyMap;
import org.gatein.pc.impl.state.StateManagementPolicyService;
import org.gatein.pc.impl.state.StateConverterV0;
import org.gatein.pc.impl.state.producer.PortletStatePersistenceManagerService;
import org.gatein.pc.impl.spi.AbstractInstanceContext;
import org.gatein.pc.api.invocation.ActionInvocation;
import org.gatein.pc.api.invocation.PortletInvocation;
import org.gatein.pc.api.state.AccessMode;
import org.gatein.pc.api.state.PropertyChange;
import org.gatein.pc.state.StateConverter;
import org.gatein.pc.state.producer.ProducerPortletInvoker;
import org.gatein.pc.state.producer.ProducerPortlet;
import org.gatein.pc.support.info.PortletInfoSupport;
import org.gatein.pc.support.PortletInvokerSupport;
import org.gatein.pc.support.PortletSupport;

import static org.jboss.unit.api.Assert.*;
import org.jboss.unit.api.pojo.annotations.Create;

import java.util.Collections;
import java.util.List;
import java.util.Set;

/**
 * @author <a href="mailto:julien@jboss.org">Julien Viet</a>
 * @version $Revision: 6691 $
 */
@SuppressWarnings("unchecked")
public abstract class ProducerStatefulPortletInvokerTestCase extends AbstractStatefulPortletInvokerTestCase
{

   protected ProducerStatefulPortletInvokerTestCase(boolean persistLocally)
   {
      super(persistLocally);
   }

   /** . */
   protected ProducerPortletInvoker producer;

   /** . */
   protected PortletInvokerSupport container;

   /** . */
   protected PortletStatePersistenceManagerService persistenceManager;

   /** . */
   protected StateManagementPolicyService stateManagementPolicy;

   /** . */
   protected StateConverter stateConverter;

   @Create
   public void create() throws Exception
   {
      producer = new ProducerPortletInvoker();
      container = new PortletInvokerSupport();
      persistenceManager = new PortletStatePersistenceManagerService();
      stateManagementPolicy = new StateManagementPolicyService();
      stateConverter = new StateConverterV0();

      //
      stateManagementPolicy.setPersistLocally(persistLocally);

      //
      producer.setNext(container);
      producer.setPersistenceManager(persistenceManager);
      producer.setStateManagementPolicy(stateManagementPolicy);
      producer.setStateConverter(stateConverter);
   }

   protected void assertNoExistingState()
   {
      assertEquals(0, persistenceManager.getSize());
   }

   protected PortletContext createPOPRef(PortletInfoSupport portletSupport) throws PortletInvokerException
   {
      container.addPortlet("PortletId", portletSupport);
      Portlet portlet = getSinglePOP();
      return portlet.getContext();
   }

   protected PortletContext createNonExistingPOPRef()
   {
      container.addPortlet("NonExistingPortletId", new PortletInfoSupport());
      PortletContext popCtx = getSinglePOP().getContext();
      container.removePortlet("NonExistingPortletId");
      return popCtx;
   }

   protected PortletContext createInvalidPOPRef()
   {
      container.addPortlet("InvalidPortletId", new PortletInfoSupport());
      PortletContext popCtx = getSinglePOP().getContext();
      container.setValid("InvalidPortletId", false);
      return popCtx;
   }

   protected PortletContext createLocalClone(PortletContext portletRef) throws Exception
   {
      stateManagementPolicy.setPersistLocally(true);
      PortletContext cloneContext = producer.createClone(portletRef);
      stateManagementPolicy.setPersistLocally(persistLocally);
      return cloneContext;
   }

   protected void addPreference(PortletContext popRef, String key, List<String> defaultValue)
   {
      container.getPortlet(popRef.getId()).addPreference(key, defaultValue);
   }

   protected void addPreference(PortletContext popRef, String key, List<String> defaultValue, Boolean readOnly)
   {
      container.getPortlet(popRef.getId()).addPreference(key, defaultValue, readOnly);
   }

   protected void destroyClone(PortletContext portletRef) throws Exception
   {
      assertEquals(Collections.EMPTY_LIST, producer.destroyClones(Collections.singletonList(portletRef)));
   }

   protected PortletContext createClone(PortletContext portletRef) throws PortletInvokerException
   {
      return producer.createClone(portletRef);
   }

   protected PortletContext setProperties(PortletContext portletRef, PropertyChange[] changes) throws PortletInvokerException
   {
      return producer.setProperties(portletRef, changes);
   }

   protected PropertyMap getProperties(PortletContext portletRef) throws PortletInvokerException
   {
      return producer.getProperties(portletRef);
   }

   protected PropertyMap getProperties(PortletContext portletRef, Set keys) throws PortletInvokerException
   {
      return producer.getProperties(portletRef, keys);
   }

   protected List destroyClones(List portletRefs) throws PortletInvokerException
   {
      return producer.destroyClones(portletRefs);
   }

   protected PortletSupport getPortletSupport(PortletContext portletRef) throws PortletInvokerException
   {
      Portlet portlet = producer.getPortlet(portletRef);

      //
      if (portlet instanceof ProducerPortlet)
      {
         portlet = ((ProducerPortlet)portlet).getNext();
      }

      //
      return (PortletSupport)portlet;
   }

   protected Portlet getPortlet(PortletContext portletRef) throws PortletInvokerException
   {
      if (portletRef == null)
      {
         return producer.getPortlet(null);
      }
      else
      {
         return producer.getPortlet(portletRef);
      }
   }

   protected String getPortletId(PortletContext portletRef) throws PortletInvokerException
   {
      return portletRef.getId();
   }

   protected void invoke(PortletInvocation invocation) throws PortletInvokerException
   {
      producer.invoke(invocation);
   }

   protected ActionInvocation createAction(PortletContext portletRef, AccessMode accessMode)
   {
      ActionContextImpl actionCtx = new ActionContextImpl();
      AbstractInstanceContext instanceCtx = new AbstractInstanceContext("blah", accessMode);

      //
      ActionInvocation action = new ActionInvocation(actionCtx);
      action.setInstanceContext(instanceCtx);
      action.setTarget(portletRef);
      return action;
   }

   protected PortletContext getImplicitClonedRef(ActionInvocation action)
   {
      AbstractInstanceContext instanceCtx = (AbstractInstanceContext)action.getInstanceContext();
      return instanceCtx.getClonedContext();
   }

   private Portlet getSinglePOP()
   {
      Set portlets = container.getPortlets();
      assertNotNull(portlets);
      assertEquals(1, portlets.size());
      return (Portlet)portlets.iterator().next();
   }
}