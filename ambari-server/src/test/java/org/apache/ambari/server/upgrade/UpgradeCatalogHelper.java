/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.ambari.server.upgrade;

import java.util.Collections;

import org.apache.ambari.server.orm.dao.ClusterDAO;
import org.apache.ambari.server.orm.dao.ClusterServiceDAO;
import org.apache.ambari.server.orm.dao.HostComponentDesiredStateDAO;
import org.apache.ambari.server.orm.dao.HostDAO;
import org.apache.ambari.server.orm.dao.ResourceTypeDAO;
import org.apache.ambari.server.orm.dao.ServiceComponentDesiredStateDAO;
import org.apache.ambari.server.orm.entities.ClusterEntity;
import org.apache.ambari.server.orm.entities.ClusterServiceEntity;
import org.apache.ambari.server.orm.entities.HostComponentDesiredStateEntity;
import org.apache.ambari.server.orm.entities.HostComponentStateEntity;
import org.apache.ambari.server.orm.entities.HostEntity;
import org.apache.ambari.server.orm.entities.ResourceEntity;
import org.apache.ambari.server.orm.entities.ResourceTypeEntity;
import org.apache.ambari.server.orm.entities.ServiceComponentDesiredStateEntity;
import org.apache.ambari.server.orm.entities.ServiceDesiredStateEntity;
import org.apache.ambari.server.state.HostComponentAdminState;
import org.apache.ambari.server.state.State;

import com.google.inject.Injector;
import com.google.inject.persist.Transactional;

/**
 * The {@link UpgradeCatalogHelper} contains utility methods to help and of the
 * {@link UpgradeCatalog} tests.
 */
public class UpgradeCatalogHelper {

  /**
   * Creates a cluster with the specified name and stack.
   *
   * @param injector
   * @param clusterName
   * @param desiredStackVersion
   * @return
   */
  protected ClusterEntity createCluster(Injector injector, String clusterName,
      String desiredStackVersion) {
    ResourceTypeDAO resourceTypeDAO = injector.getInstance(ResourceTypeDAO.class);

    // create an admin resource to represent this cluster
    ResourceTypeEntity resourceTypeEntity = resourceTypeDAO.findById(ResourceTypeEntity.CLUSTER_RESOURCE_TYPE);
    if (resourceTypeEntity == null) {
      resourceTypeEntity = new ResourceTypeEntity();
      resourceTypeEntity.setId(ResourceTypeEntity.CLUSTER_RESOURCE_TYPE);
      resourceTypeEntity.setName(ResourceTypeEntity.CLUSTER_RESOURCE_TYPE_NAME);
      resourceTypeEntity = resourceTypeDAO.merge(resourceTypeEntity);
    }

    ResourceEntity resourceEntity = new ResourceEntity();
    resourceEntity.setResourceType(resourceTypeEntity);

    ClusterDAO clusterDAO = injector.getInstance(ClusterDAO.class);
    ClusterEntity clusterEntity = new ClusterEntity();
    clusterEntity.setClusterId(1L);
    clusterEntity.setClusterName(clusterName);
    clusterEntity.setDesiredStackVersion(desiredStackVersion);
    clusterEntity.setProvisioningState(State.INIT);
    clusterEntity.setResource(resourceEntity);
    clusterDAO.create(clusterEntity);
    return clusterEntity;
  }

  /**
   * Create a new service in the specified cluster.
   *
   * @param injector
   * @param clusterEntity
   * @param serviceName
   * @return
   */
  protected ClusterServiceEntity createService(Injector injector,
      ClusterEntity clusterEntity, String serviceName) {
    ClusterServiceDAO clusterServiceDAO = injector.getInstance(ClusterServiceDAO.class);
    ClusterServiceEntity clusterServiceEntity = new ClusterServiceEntity();
    clusterServiceEntity.setClusterId(1L);
    clusterServiceEntity.setClusterEntity(clusterEntity);
    clusterServiceEntity.setServiceName(serviceName);
    clusterServiceDAO.create(clusterServiceEntity);
    return clusterServiceEntity;
  }

  /**
   * Adds the specified service to a cluster. The service must already be part
   * of the cluster.
   *
   * @param injector
   * @param clusterEntity
   * @param serviceName
   * @param desiredStackVersion
   * @return
   */
  protected ClusterServiceEntity addService(Injector injector,
      ClusterEntity clusterEntity, String serviceName,
      String desiredStackVersion) {
    ClusterDAO clusterDAO = injector.getInstance(ClusterDAO.class);

    ClusterServiceEntity clusterServiceEntity = createService(injector,
        clusterEntity, serviceName);

    ServiceDesiredStateEntity serviceDesiredStateEntity = new ServiceDesiredStateEntity();
    serviceDesiredStateEntity.setDesiredStackVersion(desiredStackVersion);
    serviceDesiredStateEntity.setClusterId(1L);
    serviceDesiredStateEntity.setServiceName(serviceName);
    serviceDesiredStateEntity.setClusterServiceEntity(clusterServiceEntity);

    clusterServiceEntity.setServiceDesiredStateEntity(serviceDesiredStateEntity);
    clusterEntity.getClusterServiceEntities().add(clusterServiceEntity);

    clusterDAO.merge(clusterEntity);

    return clusterServiceEntity;
  }

  /**
   * Create a host in the specified cluster.
   *
   * @param injector
   * @param clusterEntity
   * @param hostName
   * @return
   */
  protected HostEntity createHost(Injector injector,
      ClusterEntity clusterEntity, String hostName) {
    HostDAO hostDAO = injector.getInstance(HostDAO.class);
    ClusterDAO clusterDAO = injector.getInstance(ClusterDAO.class);
    HostEntity hostEntity = new HostEntity();
    hostEntity.setHostName(hostName);
    hostEntity.setClusterEntities(Collections.singletonList(clusterEntity));
    hostDAO.create(hostEntity);
    clusterEntity.getHostEntities().add(hostEntity);
    clusterDAO.merge(clusterEntity);
    return hostEntity;
  }

  /**
   * Adds a host component for a given service and host.
   *
   * @param injector
   * @param clusterEntity
   * @param clusterServiceEntity
   * @param hostEntity
   * @param componentName
   * @param desiredStackVersion
   */
  @Transactional
  protected void addComponent(Injector injector, ClusterEntity clusterEntity,
      ClusterServiceEntity clusterServiceEntity, HostEntity hostEntity,
      String componentName, String desiredStackVersion) {
    ServiceComponentDesiredStateEntity componentDesiredStateEntity = new ServiceComponentDesiredStateEntity();
    componentDesiredStateEntity.setClusterServiceEntity(clusterServiceEntity);
    componentDesiredStateEntity.setComponentName(componentName);
    componentDesiredStateEntity.setServiceName(clusterServiceEntity.getServiceName());
    componentDesiredStateEntity.setDesiredStackVersion(desiredStackVersion);
    componentDesiredStateEntity.setClusterServiceEntity(clusterServiceEntity);
    componentDesiredStateEntity.setClusterId(clusterServiceEntity.getClusterId());

    HostComponentDesiredStateDAO hostComponentDesiredStateDAO = injector.getInstance(HostComponentDesiredStateDAO.class);
    HostComponentDesiredStateEntity hostComponentDesiredStateEntity = new HostComponentDesiredStateEntity();
    hostComponentDesiredStateEntity.setClusterId(clusterEntity.getClusterId());
    hostComponentDesiredStateEntity.setHostName(hostEntity.getHostName());
    hostComponentDesiredStateEntity.setComponentName(componentName);
    hostComponentDesiredStateEntity.setServiceName(clusterServiceEntity.getServiceName());
    hostComponentDesiredStateEntity.setAdminState(HostComponentAdminState.INSERVICE);
    hostComponentDesiredStateEntity.setServiceComponentDesiredStateEntity(componentDesiredStateEntity);
    hostComponentDesiredStateEntity.setHostEntity(hostEntity);
    hostComponentDesiredStateDAO.create(hostComponentDesiredStateEntity);

    HostComponentStateEntity hostComponentStateEntity = new HostComponentStateEntity();
    hostComponentStateEntity.setHostEntity(hostEntity);
    hostComponentStateEntity.setHostName(hostEntity.getHostName());
    hostComponentStateEntity.setComponentName(componentName);
    hostComponentStateEntity.setServiceName(clusterServiceEntity.getServiceName());
    hostComponentStateEntity.setClusterId(clusterEntity.getClusterId());
    hostComponentStateEntity.setCurrentStackVersion(clusterEntity.getDesiredStackVersion());
    hostComponentStateEntity.setServiceComponentDesiredStateEntity(componentDesiredStateEntity);

    componentDesiredStateEntity.setHostComponentStateEntities(Collections.singletonList(hostComponentStateEntity));
    componentDesiredStateEntity.setHostComponentDesiredStateEntities(Collections.singletonList(hostComponentDesiredStateEntity));

    hostEntity.addHostComponentStateEntity(hostComponentStateEntity);
    hostEntity.addHostComponentDesiredStateEntity(hostComponentDesiredStateEntity);

    clusterServiceEntity.getServiceComponentDesiredStateEntities().add(
        componentDesiredStateEntity);

    ClusterServiceDAO clusterServiceDAO = injector.getInstance(ClusterServiceDAO.class);
    ServiceComponentDesiredStateDAO serviceComponentDesiredStateDAO = injector.getInstance(ServiceComponentDesiredStateDAO.class);
    HostDAO hostDAO = injector.getInstance(HostDAO.class);
    serviceComponentDesiredStateDAO.merge(componentDesiredStateEntity);
    hostDAO.merge(hostEntity);
    clusterServiceDAO.merge(clusterServiceEntity);
  }
}
