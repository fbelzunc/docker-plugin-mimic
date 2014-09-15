package com.nirima.jenkins.plugins.docker;

import com.cloudbees.jenkins.plugins.sshcredentials.SSHAuthenticator;
import com.cloudbees.jenkins.plugins.sshcredentials.SSHUserListBoxModel;
import com.cloudbees.plugins.credentials.CredentialsProvider;
import com.cloudbees.plugins.credentials.common.StandardUsernameCredentials;
import com.google.common.base.Joiner;
import com.google.common.base.Objects;
import com.google.common.base.Strings;

import org.kohsuke.stapler.StaplerRequest;
import net.sf.json.JSONObject;
import hudson.model.Descriptor.FormException;

import com.trilead.ssh2.Connection;
import hudson.Extension;
import hudson.Util;
import hudson.model.*;
import hudson.model.labels.LabelAtom;
import hudson.plugins.sshslaves.SSHLauncher;
import hudson.security.ACL;
import hudson.slaves.ComputerLauncher;
import hudson.slaves.NodeProperty;
import hudson.slaves.RetentionStrategy;
import hudson.util.ListBoxModel;
import hudson.util.StreamTaskListener;
import jenkins.model.Jenkins;
import org.kohsuke.stapler.AncestorInPath;
import org.kohsuke.stapler.DataBoundConstructor;

import java.io.IOException;
import java.io.PrintStream;
import java.util.*;
import java.util.logging.Logger;


public class DockerTemplate implements Describable<DockerTemplate> {
    private static final Logger LOGGER = Logger.getLogger(DockerTemplate.class.getName());


    public final String image;
    public final String labelString;

    // SSH settings
    /**
     * The id of the credentials to use.
     */
    public final String credentialsId;

    /**
     * Field dockerCommand
     */
    public final String dockerCommand;
    
    /**
     * Field lxcConfString
     */
    public final String lxcConfString;

    /**
     * Minutes before terminating an idle slave
     */
    public final String idleTerminationMinutes;

    /**
     * Field jvmOptions.
     */
    public final String jvmOptions;

    /**
     * Field javaPath.
     */
    public final String javaPath;

    /**
     * Field prefixStartSlaveCmd.
     */
    public final String prefixStartSlaveCmd;

    /**
     *  Field suffixStartSlaveCmd.
     */
    public final String suffixStartSlaveCmd;

    /**
     *  Field remoteFSMapping.
     */
    public final String remoteFsMapping;

    public final String remoteFs; // = "/home/jenkins";

    public final String hostname;

    public final int instanceCap;
    public final String[] dnsHosts;
    public final String[] volumes;
    public final String volumesFrom;

    private transient /*almost final*/ Set<LabelAtom> labelSet;

    public final boolean privileged;

    @DataBoundConstructor
    public DockerTemplate(String image, String labelString,
                          String remoteFs,
                          String remoteFsMapping,
                          String credentialsId, String idleTerminationMinutes,
                          String jvmOptions, String javaPath,
                          String prefixStartSlaveCmd, String suffixStartSlaveCmd,
                          String instanceCapStr, String dnsString,
                          String dockerCommand,
                          String volumesString, String volumesFrom,
                          String lxcConfString,
                          String hostname,
                          boolean privileged

    ) {
        this.image = image;
        this.labelString = Util.fixNull(labelString);
        this.credentialsId = credentialsId;
        System.out.println("DockerTemplate - credentialsId " + credentialsId);
        this.idleTerminationMinutes = idleTerminationMinutes;
        this.jvmOptions = jvmOptions;
        this.javaPath = javaPath;
        this.prefixStartSlaveCmd = prefixStartSlaveCmd;
        this.suffixStartSlaveCmd = suffixStartSlaveCmd;
        this.remoteFs =  Strings.isNullOrEmpty(remoteFs)?"/home/jenkins":remoteFs;
        this.remoteFsMapping = remoteFsMapping;

        this.dockerCommand = dockerCommand;
        this.lxcConfString = lxcConfString;
        this.privileged = privileged;
        this.hostname = hostname;

        if(instanceCapStr.equals("")) {
            this.instanceCap = Integer.MAX_VALUE;
        } else {
            this.instanceCap = Integer.parseInt(instanceCapStr);
        }

        this.dnsHosts = splitAndFilterEmpty(dnsString);
        this.volumes = splitAndFilterEmpty(volumesString);
        this.volumesFrom = volumesFrom;

        readResolve();
    }

    private String[] splitAndFilterEmpty(String s) {
        List<String> temp = new ArrayList<String>();
        for (String item : s.split(" ")) {
            if (!item.isEmpty())
                temp.add(item);
        }

        return temp.toArray(new String[temp.size()]);

    }

    public String getInstanceCapStr() {
        if (instanceCap==Integer.MAX_VALUE) {
            return "";
        } else {
            return String.valueOf(instanceCap);
        }
    }

    public String getDnsString() {
        return Joiner.on(" ").join(dnsHosts);
    }

    public String getVolumesString() {
	return Joiner.on(" ").join(volumes);
    }

    public String getVolumesFrom() {
        return volumesFrom;
    }

    public String getRemoteFsMapping() {
        return remoteFsMapping;
    }

    public Descriptor<DockerTemplate> getDescriptor() {
        return Jenkins.getInstance().getDescriptor(getClass());
    }

    public Set<LabelAtom> getLabelSet(){
        return labelSet;
    }

    /**
     * Initializes data structure that we don't persist.
     */
    protected Object readResolve() {
        labelSet = Label.parse(labelString);
        return this;
    }

    public String getDisplayName() {
        return "Image of " + image;
    }

    @Extension
    public static final class DescriptorImpl extends Descriptor<DockerTemplate> {

        @Override
        public String getDisplayName() {
            return "Docker Template";
        }

        public ListBoxModel doFillCredentialsIdItems(@AncestorInPath ItemGroup context) {

            return new SSHUserListBoxModel().withMatching(SSHAuthenticator.matcher(Connection.class),
                    CredentialsProvider.lookupCredentials(StandardUsernameCredentials.class, context,
                            ACL.SYSTEM, SSHLauncher.SSH_SCHEME));
        }

        @Override
        public boolean configure(StaplerRequest staplerRequest, JSONObject json) throws FormException {
            // to persist global configuration information,
            // set that to properties and call save().

          System.out.println("Dockertemplate - configure");

          save();
          
          return true;
        }
    }

    @Override
    public String toString() {
        return Objects.toStringHelper(this)
                .add("image", image)
                .toString();
    }
}
