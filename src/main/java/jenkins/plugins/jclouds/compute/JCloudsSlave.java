package jenkins.plugins.jclouds.compute;

import hudson.Extension;
import hudson.model.Computer;
import hudson.model.Descriptor;
import hudson.model.Slave;
import hudson.slaves.ComputerLauncher;
import hudson.slaves.NodeProperty;
import hudson.slaves.RetentionStrategy;
import org.jclouds.compute.ComputeService;
import org.jclouds.compute.domain.NodeMetadata;
import org.kohsuke.stapler.DataBoundConstructor;

import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.logging.Logger;

/**
 * Jenkins Slave node  - managed by JClouds.
 *
 * @author Vijay Kiran
 */
public class JCloudsSlave extends Slave {
    private static final Logger LOGGER = Logger.getLogger(JCloudsSlave.class.getName());
    private NodeMetadata nodeMetaData;
    public final boolean stopOnTerminate;
    
   @DataBoundConstructor
   public JCloudsSlave(String name,
                       String nodeDescription,
                       String remoteFS,
                       String numExecutors,
                       Mode mode,
                       String labelString,
                       ComputerLauncher launcher,
                       RetentionStrategy retentionStrategy,
                       List<? extends NodeProperty<?>> nodeProperties,
                       boolean stopOnTerminate) throws Descriptor.FormException, IOException {
      super(name, nodeDescription, remoteFS, numExecutors, mode, labelString, launcher, retentionStrategy, nodeProperties);
      this.stopOnTerminate = stopOnTerminate;
   }

   /**
    * Constructs a new slave from JCloud's NodeMetadata
    *
    * @param metadata - JCloudsNodeMetadata
    * @param labelString - Label(s) for this slave.
    * @param description - Description of this slave.
    * @param numExecutors - Number of executors for this slave.
    * @param stopOnTerminate - if true, suspend the slave rather than terminating it.
    * @throws IOException
    * @throws Descriptor.FormException
    */
    public JCloudsSlave(NodeMetadata metadata, final String labelString,
                        final String description, final String numExecutors,
                        final boolean stopOnTerminate) throws IOException, Descriptor.FormException {
      this(metadata.getName(),
           description,
           "/jenkins",
           numExecutors,
           Mode.NORMAL,
           labelString,
           new JCloudsLauncher(),
           new JCloudsRetentionStrategy(),
           Collections.<NodeProperty<?>>emptyList(),
           stopOnTerminate);
      this.nodeMetaData = metadata;
   }

   /**
    * Get Jclouds NodeMetadata associated with this Slave.
    *
    * @return {@link NodeMetadata}
    */
   public NodeMetadata getNodeMetaData() {
      return nodeMetaData;
   }

   /**
    * {@inheritDoc}
    */
   @Override
   public Computer createComputer() {
      LOGGER.info("Creating a new JClouds Slave");
      return new JCloudsComputer(this);
   }

   /**
    * Destroy the node calls {@link ComputeService#destroyNode}
    *
    */
   public void terminate() {
       if (stopOnTerminate) {
           LOGGER.info("Suspending the Slave : " + getNodeName());
           final ComputeService compute = JCloudsCloud.get().getCompute();
           compute.suspendNode(nodeMetaData.getId());
       } else {
           LOGGER.info("Terminating the Slave : " + getNodeName());
           final ComputeService compute = JCloudsCloud.get().getCompute();
           compute.destroyNode(nodeMetaData.getId());
       }
   }


   @Extension
   public static final class JCloudsSlaveDescriptor extends SlaveDescriptor {

      @Override
      public String getDisplayName() {
         return "JClouds Slave";
      }


      /**
       * {@inheritDoc}
       */
      @Override
      public boolean isInstantiable() {
         return false;
      }
   }
}
