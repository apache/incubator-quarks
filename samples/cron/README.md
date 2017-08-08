Restarting Edgent if the JVM crashes

The startapp.sh script can be setup to run as a cron job every minute in order
to monitor a JVM running an Edgent application and restart Edgent if the 
JVM crashes. The script checks whether the pid of the JVM indicates
a process which is still running.  If the pid is not there, it executes the 
command to start the application in the first place.

A crontab entry file contains information which cron uses to schedule the job.
The sample startapp.cron file is configured to execute the 
org.apache.edgent.samples.topology.TerminateAfterNTuples sample application,
which terminates the JVM after processing a preset number of tuples.

See README.md in the samples root directory for information on building the samples.

To setup cron to restart the sample application every minute:

1. Create the startapp.cron file from the startapp.cron.template file:

   $ ./mkcrontab

2. Install startapp.cron:

   $ crontab ./startapp.cron

   Note: if you wish to have your ~/.profile executed you must explicitly
   do so in the crontab entry or in a script called by the entry.

3. To see the set crontab entries:

   $ crontab -l

3. To remove the current crontab entries:

   $ crontab -r
