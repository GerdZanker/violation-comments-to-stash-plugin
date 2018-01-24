package org.jenkinsci.plugins.jvctb;

import static com.google.common.base.Strings.isNullOrEmpty;
import static org.jenkinsci.plugins.jvctb.config.ViolationsToBitbucketServerConfigHelper.FIELD_BITBUCKETSERVERURL;
import static org.jenkinsci.plugins.jvctb.config.ViolationsToBitbucketServerConfigHelper.FIELD_COMMENTONLYCHANGEDCONTENT;
import static org.jenkinsci.plugins.jvctb.config.ViolationsToBitbucketServerConfigHelper.FIELD_COMMENTONLYCHANGEDCONTENTCONTEXT;
import static org.jenkinsci.plugins.jvctb.config.ViolationsToBitbucketServerConfigHelper.FIELD_CREATECOMMENTWITHALLSINGLEFILECOMMENTS;
import static org.jenkinsci.plugins.jvctb.config.ViolationsToBitbucketServerConfigHelper.FIELD_CREATESINGLEFILECOMMENTS;
import static org.jenkinsci.plugins.jvctb.config.ViolationsToBitbucketServerConfigHelper.FIELD_KEEP_OLD_COMMENTS;
import static org.jenkinsci.plugins.jvctb.config.ViolationsToBitbucketServerConfigHelper.FIELD_MINSEVERITY;
import static org.jenkinsci.plugins.jvctb.config.ViolationsToBitbucketServerConfigHelper.FIELD_PASSWORD;
import static org.jenkinsci.plugins.jvctb.config.ViolationsToBitbucketServerConfigHelper.FIELD_PATTERN;
import static org.jenkinsci.plugins.jvctb.config.ViolationsToBitbucketServerConfigHelper.FIELD_PROJECTKEY;
import static org.jenkinsci.plugins.jvctb.config.ViolationsToBitbucketServerConfigHelper.FIELD_PULLREQUESTID;
import static org.jenkinsci.plugins.jvctb.config.ViolationsToBitbucketServerConfigHelper.FIELD_REPORTER;
import static org.jenkinsci.plugins.jvctb.config.ViolationsToBitbucketServerConfigHelper.FIELD_REPOSLUG;
import static org.jenkinsci.plugins.jvctb.config.ViolationsToBitbucketServerConfigHelper.FIELD_USERNAME;
import static org.jenkinsci.plugins.jvctb.config.ViolationsToBitbucketServerConfigHelper.FIELD_USERNAMEPASSWORDCREDENTIALSID;
import static org.jenkinsci.plugins.jvctb.config.ViolationsToBitbucketServerConfigHelper.FIELD_USEUSERNAMEPASSWORD;
import static org.jenkinsci.plugins.jvctb.config.ViolationsToBitbucketServerConfigHelper.FIELD_USEUSERNAMEPASSWORDCREDENTIALS;
import static org.jenkinsci.plugins.jvctb.config.ViolationsToBitbucketServerConfigHelper.createNewConfig;

import hudson.Extension;
import hudson.model.AbstractProject;
import hudson.tasks.BuildStepDescriptor;
import hudson.tasks.Publisher;
import hudson.util.ListBoxModel;

import java.util.List;

import net.sf.json.JSONObject;

import org.jenkinsci.Symbol;
import org.jenkinsci.plugins.jvctb.config.CredentialsHelper;
import org.jenkinsci.plugins.jvctb.config.ViolationConfig;
import org.jenkinsci.plugins.jvctb.config.ViolationsToBitbucketServerConfig;
import org.kohsuke.stapler.StaplerRequest;

import se.bjurr.violations.lib.model.SEVERITY;

@Extension
@Symbol("ViolationsToBitbucketServer")
public final class ViolationsToBitbucketServerDescriptor extends BuildStepDescriptor<Publisher> {
  private ViolationsToBitbucketServerConfig config;

  public ViolationsToBitbucketServerDescriptor() {
    super(ViolationsToBitbucketServerRecorder.class);
    load();
    if (config == null
        || config.getViolationConfigs().size() != createNewConfig().getViolationConfigs().size()) {
      config = createNewConfig();
    }
  }

  public ListBoxModel doFillUsernamePasswordCredentialsIdItems() {
    return CredentialsHelper.doFillUsernamePasswordCredentialsIdItems();
  }

  @Override
  public String getDisplayName() {
    return "Report Violations to Bitbucket Server";
  }

  @Override
  public String getHelpFile() {
    return super.getHelpFile();
  }

  /** Create new blank configuration. Used when job is created. */
  public ViolationsToBitbucketServerConfig getNewConfig() {
    return createNewConfig();
  }

  @Override
  public boolean isApplicable(
      @SuppressWarnings("rawtypes") final Class<? extends AbstractProject> jobType) {
    return true;
  }

  @SuppressWarnings("unchecked")
  @Override
  public Publisher newInstance(StaplerRequest req, JSONObject formData)
      throws hudson.model.Descriptor.FormException {
    final ViolationsToBitbucketServerConfig config = createNewConfig();
    config.setBitbucketServerUrl(formData.getString(FIELD_BITBUCKETSERVERURL));
    config.setRepoSlug(formData.getString(FIELD_REPOSLUG));
    config.setProjectKey(formData.getString(FIELD_PROJECTKEY));
    config.setPullRequestId(formData.getString(FIELD_PULLREQUESTID));

    config.setUseUsernamePassword(formData.getBoolean(FIELD_USEUSERNAMEPASSWORD));
    config.setUsername(formData.getString(FIELD_USERNAME));
    config.setPassword(formData.getString(FIELD_PASSWORD));

    config.setUseUsernamePasswordCredentials(
        formData.getBoolean(FIELD_USEUSERNAMEPASSWORDCREDENTIALS));
    config.setUsernamePasswordCredentialsId(
        formData.getString(FIELD_USERNAMEPASSWORDCREDENTIALSID));

    config.setCreateCommentWithAllSingleFileComments(
        formData.getString(FIELD_CREATECOMMENTWITHALLSINGLEFILECOMMENTS).equalsIgnoreCase("true"));

    config.setCreateSingleFileComments(
        formData.getString(FIELD_CREATESINGLEFILECOMMENTS).equalsIgnoreCase("true"));
    config.setCommentOnlyChangedContent(
        formData.getString(FIELD_COMMENTONLYCHANGEDCONTENT).equalsIgnoreCase("true"));
    config.setCommentOnlyChangedContentContext(
        formData.getInt(FIELD_COMMENTONLYCHANGEDCONTENTCONTEXT));

    final String minSeverityString = formData.getString(FIELD_MINSEVERITY);
    config.setKeepOldComments(formData.getString(FIELD_KEEP_OLD_COMMENTS).equalsIgnoreCase("true"));
    if (!isNullOrEmpty(minSeverityString)) {
      config.setMinSeverity(SEVERITY.valueOf(minSeverityString));
    } else {
      config.setMinSeverity(null);
    }

    int i = 0;
    final List<String> patterns = (List<String>) formData.get(FIELD_PATTERN);
    final List<String> reporters = (List<String>) formData.get(FIELD_REPORTER);
    for (final String pattern : patterns) {
      final ViolationConfig violationConfig = config.getViolationConfigs().get(i);
      violationConfig.setPattern(pattern);
      final String reporter = reporters.get(i);
      violationConfig.setReporter(reporter);
      i++;
    }
    final ViolationsToBitbucketServerRecorder publisher = new ViolationsToBitbucketServerRecorder();
    publisher.setConfig(config);
    return publisher;
  }
}
