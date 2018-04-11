package net.whn.loki.common;

import net.whn.loki.commandLine.CommandLineHelper;
import net.whn.loki.common.ICommon.LokiRole;
import net.whn.loki.common.configs.Config;
import net.whn.loki.error.DefaultExceptionHandler;
import net.whn.loki.error.ErrorHelper;
import net.whn.loki.grunt.GruntForm;
import net.whn.loki.grunt.GruntR;
import net.whn.loki.io.IOHelper;
import net.whn.loki.master.AddJobForm;
import net.whn.loki.master.AnnouncerR;
import net.whn.loki.master.MasterForm;
import net.whn.loki.master.MasterR;

import javax.swing.*;
import java.awt.*;
import java.io.File;
import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.logging.*;

public class Main {

    private static final String className = Main.class.toString();
    private static final Logger log = Logger.getLogger(className);

    private static final long serialVersionUID = 14;
    public static final String LOKI_VERSION = String.format("R0.7.3.%03d", serialVersionUID);
    private static String lokiVersionInitials = LOKI_VERSION.substring(0, 7); // like: "R0.7.3"

    private static boolean isGruntFromCommandLine = false;
    private static DefaultExceptionHandler defaultHandler;
    private static int masterMessageQueueSize = 100;

    //master
    private static MasterR master;
    private static Thread masterThread;
    private static MasterForm masterForm;
    //grunt
    private static GruntR gruntR;
    private static GruntForm gruntForm;
    private static InetAddress manualMasterIP;
    private static boolean  autoDiscoverMaster = true;

    private static String commandLineTemplate = getCommandLineTemplate();
    private static String blenderExe;

    public static File lokiBaseFolder;

    private static File gruntMainFolder;
    private static File gruntCacheFolder;
    private static File gruntRenderFolder;

    private static File masterMainFolder;
    private static File masterCacheFolder;
    private static File masterTempFolder;

    public static LokiRole myRole;
    private static Config config ;
    private static String lokiVersionsErrorTitle = "Loki couldn't start.";
    private static String lokiVersionsErrorTemplate = "Loki Config file version '%s', currently application version is '%s'.";
    private static String lokiVersionSuggestion = "Config file must be updated. Install updated config file and restart?";
    private static String lokiVersionSuggestionCommandLineGrunt = "Updating config file ...";
    private static String closeApplicationMessage = "Application closed, because of incompatible versions!";
    public static String differentLokiVersionsErrorTemplate = "Incompatible application versions! Grunt(%s) vs Master(%s)";
    public static final String LOKI_BASE_FOLDER = ".loki";

    public static void main(String[] args) {

        if (args.length > 0) {
            if (checkGruntFolders()) {
                handleCommandLineArgs(args);
                if (couldSetupRunningLock()) {
                    setupLogging();
                    loadGruntCommandLineConfig();
                    startLoki();
                }
            }
        } else { // will have GUI
            LokiForm lokiForm = getLokiForm();
            if (checkLokiBaseFolder(lokiForm) && couldSetupRunningLock(lokiForm)) {
                setupLogging();
                if (couldLoadConfig() && hasChosenLokiRole(lokiForm)) {
                    startLoki(lokiForm);
                }
            }
        }
    }

    private static boolean checkLokiBaseFolder(LokiForm lokiForm) {
        if ((lokiBaseFolder = IOHelper.getLokiBaseFolder()) != null) {
            return true;
        } else {
            IOHelper.handleUnwrittableFileSystemException(lokiForm);
            return false;
        }
    }

    private static boolean checkOtherGruntFolders(LokiForm lokiForm) {

        if ((gruntMainFolder = IOHelper.getGruntMainFolder(lokiBaseFolder)) != null
                && (gruntCacheFolder = IOHelper.getGruntCacheFolder(gruntMainFolder)) != null
                && (gruntRenderFolder = IOHelper.getGruntRenderFolder(gruntMainFolder)) != null) {

            GruntR.setupWorkingGruntFolders(gruntMainFolder, gruntCacheFolder, gruntRenderFolder);

            return true;
        } else {
            IOHelper.handleUnwrittableFileSystemException(lokiForm);
            return false;
        }
    }

    private static boolean checkOtherMasterFolders(LokiForm lokiForm) {

        if ((masterMainFolder = IOHelper.getMasterMainFolder(lokiBaseFolder)) != null
                && (masterCacheFolder = IOHelper.getMasterCacheFolder(masterMainFolder)) != null
                && (masterTempFolder = IOHelper.getMasterTempFolder(masterMainFolder)) != null) {

            MasterR.setupWorkingMasterFolders(masterMainFolder, masterCacheFolder, masterTempFolder);

            return true;
        } else {
            IOHelper.handleUnwrittableFileSystemException(lokiForm);
            return false;
        }
    }

    private static boolean checkGruntFolders() {

        if ((lokiBaseFolder = IOHelper.getLokiBaseFolder()) != null
                && (gruntMainFolder = IOHelper.getGruntMainFolder(lokiBaseFolder)) != null
                && (gruntCacheFolder = IOHelper.getGruntCacheFolder(gruntMainFolder)) != null
                && (gruntRenderFolder = IOHelper.getGruntRenderFolder(gruntMainFolder)) != null) {

            GruntR.setupWorkingGruntFolders(gruntMainFolder, gruntCacheFolder, gruntRenderFolder);
            return true;
        } else {
            IOHelper.handleUnwrittableFileSystemException();
            return false;
        }
    }

    private static LokiForm getLokiForm() {

        LokiForm lokiForm = new LokiForm();
        setNativeLookAndFeel();

        defaultHandler = new DefaultExceptionHandler(lokiForm);
        Thread.setDefaultUncaughtExceptionHandler(defaultHandler);

        return lokiForm;
    }

    private static boolean couldSetupRunningLock() {

        boolean launchLoki = true;
        try {
            if (!IOHelper.couldSetupRunningLock(lokiBaseFolder)) {
                System.out.println(
                        new StringBuilder("Already running or improper shutdown.")
                                .append(System.lineSeparator())
                                .append("If Loki was improperly shutdown, you will need")
                                .append(System.lineSeparator())
                                .append("to delete the .loki/.runningLock file.")
                );
                log.warning("already running, or improper shutdown");
            }
        } catch (IOException e) {
            System.out.println(getIoMessage(e));
        }

        return launchLoki;
    }

    private static String getIoMessage(IOException ex) {

        return new StringBuilder("Loki Render is having IO problems with the filesystem:")
                .append(System.lineSeparator())
                .append(ex.getMessage())
                .append(System.lineSeparator())
                .append("Click OK to exit.")
                .toString();
    }

    private static boolean couldSetupRunningLock(LokiForm lokiForm) {

        boolean launchLoki = true;

        try {
            if (!IOHelper.couldSetupRunningLock(lokiBaseFolder)) {
                String approveBtn = "Start";
                String cancelBtn = "Quit";
                Object[] options = {approveBtn, cancelBtn};

                String alreadyRunningText = "Loki is already running, or wasn't properly shutdown.\n" +
                        "If Loki isn't running, you can safely select 'Start'";

                String title = "Already running, or improper shutdown";

                launchLoki = JOptionPane.showOptionDialog(lokiForm,
                        alreadyRunningText,
                        title,
                        JOptionPane.YES_NO_OPTION,
                        JOptionPane.WARNING_MESSAGE,
                        null,
                        options,
                        approveBtn
                ) == JFileChooser.APPROVE_OPTION;
                log.warning(title);
            }
        } catch (IOException e) {
            handleIOException(e, lokiForm);
        }

        return launchLoki;
    }

    private static void handleIOException(IOException ex, LokiForm lokiForm) {

        JOptionPane.showMessageDialog(lokiForm, getIoMessage(ex));
        lokiForm.dispose();
    }

    /**
     * fatal error during Announce startup
     * @param lokiForm
     * @param ex
     */
    private static void handleFatalException(LokiForm lokiForm, IOException ex) {
        String errMsg = "Loki encountered a fatal error.\n" + "Click OK to exit.";
        ErrorHelper.outputToLogMsgAndKill(lokiForm, isGruntFromCommandLine, log, errMsg, ex);
        System.exit(-1);
    }

    private static void loadGruntCommandLineConfig() {
        config = loadConfig();
//        config.setLOKI_VERSION("R0.7.3.007");
//        config.setLOKI_VERSION("0.7.2");

        // local checking (by the version from jar)
        String oldLokiVersion = config.getLOKI_VERSION();
        if (oldLokiVersion.startsWith(lokiVersionInitials)) {
            if (!oldLokiVersion.equals(LOKI_VERSION)) {

                log.warning(new StringBuilder(getDifferentLokiVersionsMessage())
                        .append(System.lineSeparator())
                        .append("   ").append(lokiVersionSuggestionCommandLineGrunt)
                        .toString()
                );
                updateConfigFile();
            }

            config.setBlenderBin(blenderExe);
            System.out.println("Starting Loki grunt in command line mode");
            System.out.println("Attempting to connect to master...");

        } else {
            log.warning(new StringBuilder(getDifferentLokiVersionsMessage())
                    .append(System.lineSeparator())
                    .append(closeApplicationMessage)
                    .toString()
            );
            System.exit(1);
        }
    }

    private static String getDifferentLokiVersionsMessage() {
        return String.format(lokiVersionsErrorTemplate, config.getLOKI_VERSION(), Main.LOKI_VERSION);
    }

    private static Config loadConfig() {

        Config config = Config.readConfigFile(lokiBaseFolder);

        if (!autoDiscoverMaster) {
            config.setMasterIp(manualMasterIP);
            config.setAutoDiscoverMaster(false);
        }

        return config;
    }

    private static boolean couldLoadConfig() {

        boolean result = false;
        config = loadConfig();
//        config.setLOKI_VERSION("R0.7.3.007");
//        config.setLOKI_VERSION("0.7.2");

        String oldLokiVersion = config.getLOKI_VERSION();
        if (oldLokiVersion.startsWith(lokiVersionInitials)) {
            if (oldLokiVersion.equals(LOKI_VERSION)) {
                result = true;
            } else {
                JPanel panel = new JPanel();

                GridLayout gridLayout = new GridLayout(2, 1);
                panel.setLayout(gridLayout);

                String differentLokiVersionsMessage = getDifferentLokiVersionsMessage();

                panel.add(new JLabel(differentLokiVersionsMessage));
                panel.add(new JLabel(lokiVersionSuggestion));

                Dimension preferredButtonSize = new Dimension(135, 25);

                JButton okButton = new JButton("Update and restart");
                okButton.setPreferredSize(preferredButtonSize);
                okButton.addActionListener(e -> {
                    JOptionPane pane = AddJobForm.getOptionPane((JComponent) e.getSource());
                    pane.setValue(okButton);
                });

                JButton cancelButton = new JButton("Cancel");
                cancelButton.setPreferredSize(preferredButtonSize);
                cancelButton.addActionListener(e -> {
                    JOptionPane pane = AddJobForm.getOptionPane((JComponent) e.getSource());
                    pane.setValue(cancelButton);
                });

                boolean wasPressedOkBtn = JOptionPane.showOptionDialog(
                        null,
                        panel,
                        lokiVersionsErrorTitle,
                        JOptionPane.OK_CANCEL_OPTION,
                        JOptionPane.ERROR_MESSAGE,
                        null,
                        new Object[]{okButton, cancelButton},
                        okButton
                ) == JOptionPane.YES_OPTION;

                System.out.println("wasPressedOkBtn: " + wasPressedOkBtn);

                log.warning(new StringBuilder(differentLokiVersionsMessage)
                        .append(System.lineSeparator())
                        .append("   ").append(lokiVersionSuggestion).append("   {").append(wasPressedOkBtn).append("}")
                        .toString()
                );

                if (wasPressedOkBtn) {
                    updateConfigFile();
                    result = true;
                } else {
                    System.exit(1);
                }
            }
        } else {
            System.out.println(closeApplicationMessage);
        }
        return result;
    }

    private static void updateConfigFile() {
        config.deleteLokiConfigFile();
        Config newConfig = loadConfig();
        config = newConfig.initializeFrom(config);
    }

    private enum MachineRoles {

        GRUNT("Grunt", 0), MASTER("Master", 1), MASTER_AND_GRUNT("Master and Grunt", 2), NOTHING_SELECTED(null, -1);

        private String buttonCaption;
        private int index;

        MachineRoles(String buttonCaption, int index) {
            this.buttonCaption = buttonCaption;
            this.index = index;
        }

        public String getButtonCaption() {
            return buttonCaption;
        }

        public static MachineRoles get(int index) {
            switch (index) {
                case 0: return GRUNT;
                case 1: return MASTER;
                case 2: return MASTER_AND_GRUNT;
                default: return NOTHING_SELECTED;
            }
        }
    }

    private static boolean checkSettingsForGruntRole(LokiForm lokiForm) {

        if (checkOtherGruntFolders(lokiForm)) {
            if (!CommandLineHelper.couldDetermineBlenderBin(config)) {
                System.exit(0);
            }
            myRole = LokiRole.GRUNT;
            return true;
        }
        return false;
    }

    private static boolean checkSettingsForMasterRole(LokiForm lokiForm) {

        if (checkOtherMasterFolders(lokiForm)) {
            myRole = LokiRole.MASTER;
            return true;
        }
        return false;
    }

    private static boolean checkSettingsForMasterAndGruntRole(LokiForm lokiForm) {

        if (checkOtherMasterFolders(lokiForm) && checkOtherGruntFolders(lokiForm)) {
            if (!CommandLineHelper.couldDetermineBlenderBin(config)) {
                System.exit(0);
            }
            myRole = LokiRole.MASTER_GRUNT;
            return true;
        }
        return false;
    }

    private static boolean hasChosenLokiRole(LokiForm lokiForm) {

        boolean result = true;
        LokiRole configLokiRole = config.getLokiRole();

        switch (configLokiRole) {

            case GRUNT:
                result = checkSettingsForGruntRole(lokiForm);
                break;
            case MASTER:
                result = checkSettingsForMasterRole(lokiForm);
                break;
            case MASTER_GRUNT:
                result = checkSettingsForMasterAndGruntRole(lokiForm);
                break;
            case ASK:
                switch (selectRole(lokiForm)) {

                    case GRUNT:
                        result = checkSettingsForGruntRole(lokiForm);
                        break;
                    case MASTER:
                        result = checkSettingsForMasterRole(lokiForm);
                        break;
                    case MASTER_AND_GRUNT:
                        result = checkSettingsForMasterAndGruntRole(lokiForm);
                        break;
                    case NOTHING_SELECTED:
                        log.fine("quit dialog; exiting.");
                        System.exit(0);
                }
        }

        return result;
    }

    private static void startLoki() {
        startCommandLineGrunt();
    }

    private static void startLoki(LokiForm lokiForm) {
        try {
            switch (myRole) {

                case GRUNT:
                    startGrunt();
                    break;
                case MASTER:
                    startMaster(lokiForm);
                    break;
                case MASTER_GRUNT:
                    startMasterWithLocalGrunt(lokiForm);
            }
        } catch (IOException ex) {
            handleFatalException(lokiForm, ex);
        }
    }

    private static MachineRoles selectRole(LokiForm hiddenForm) {

        String[] options = {
                MachineRoles.GRUNT.getButtonCaption(),
                MachineRoles.MASTER.getButtonCaption(),
                MachineRoles.MASTER_AND_GRUNT.getButtonCaption()
        };

        return MachineRoles.get(JOptionPane.showOptionDialog(hiddenForm,
                "Please select Loki's role on this computer.",
                "Loki role",
                0,
                JOptionPane.QUESTION_MESSAGE,
                null,
                options,
                MachineRoles.GRUNT.getButtonCaption()
        ));
    }

    private static void setNativeLookAndFeel() {
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
            log.logp(Level.FINEST, className, "setNativeLookAndFeel()", "Successfully set native look and feel for GUI.");
        } catch (Exception ex) {
            //we can live w/ ugly 'metal' theme...moving on
        }
    }

    /**
     *
     * @param lokiForm
     * @throws IOException
     * -from AnnounceR() *FATAL*
     */
    private static void startMaster(LokiForm lokiForm) throws IOException {

        AnnouncerR announcer = new AnnouncerR(config, lokiForm);
        master = new MasterR(config, announcer, masterMessageQueueSize);
        masterThread = new Thread(master, "master");

        masterForm = new MasterForm(master);
        master.setMasterForm(masterForm);

        masterThread.start();
        masterForm.setVisible(true);
    }

    private static void startMasterWithLocalGrunt(LokiForm hiddenForm) throws IOException {

        startMaster(hiddenForm);

        Point p = masterForm.getLocation();
        Point gPoint = new Point(p.x, (p.y + masterForm.getHeight()));

        startGrunt(gPoint);
    }

    private static void startCommandLineGrunt() {
        gruntR = new GruntR(config);
        Thread gruntReceiverThread = new Thread(gruntR, "grunt");
        gruntReceiverThread.start();
    }

    private static Thread buildGruntR() {
        gruntR = new GruntR(master, config);
        Thread gruntReceiverThread = new Thread(gruntR, "grunt");

        gruntForm = new GruntForm(gruntR);
        gruntR.setGruntForm(gruntForm);

        return gruntReceiverThread;
    }

    private static void startGrunt() {

        Thread gruntReceiverThread = buildGruntR();
        gruntForm.setLocationRelativeTo(null);
        gruntForm.setVisible(true);

        gruntReceiverThread.start();
    }

    private static void startGrunt(Point myPoint) {

        Thread gruntReceiverThread = buildGruntR();
        gruntForm.setLocation(myPoint);
        gruntForm.setVisible(true);

        gruntReceiverThread.start();
    }

    private static void setupLogging() {

        if (System.getProperty("java.util.logging.config.class") == null && System.getProperty("java.util.logging.config.file") == null) {
            try {
                final int LOG_ROTATION_COUNT = 2;
                final int fileSize = 500_000;
                String logFilePath = new File(lokiBaseFolder, "loki.log").toString();
                Handler handler = new FileHandler(logFilePath, fileSize, LOG_ROTATION_COUNT, true);
                handler.setFormatter(new SimpleFormatter());
                Logger.getLogger("").addHandler(handler);
            } catch (IOException ex) {
                log.log(Level.SEVERE, "Can't create log file handler", ex);
            }

            /**
             * set default console handler to finest -TODO not for production
             */
            // Handler for console (reuse it if it already exists)
            Handler consoleHandler = null;
            //see if there is already a console handler
            for (Handler handler : Logger.getLogger("").getHandlers()) {
                if (handler instanceof ConsoleHandler) {
                    //found the console handler
                    consoleHandler = handler;
                    break;
                }
            }

            if (consoleHandler == null) {
                //there was no console handler found, create a new one
                consoleHandler = new ConsoleHandler();
                Logger.getLogger("").addHandler(consoleHandler);
            }
            //set the console handler to fine:
            consoleHandler.setLevel(Level.FINEST);

            //last of all, set this class's log to log all
            //log.setLevel(Level.ALL);
        }
    }

    /**
     * If it's a valid IP adress, but trying to connect to it, will result in a UnknownHostException - will exit from JVM
     * @param IP
     * @return
     */
    private static boolean isValidIP(String IP) {

        boolean isValid = false;

        if (PreferencesForm.isValidIP(IP)) {
            try {
                manualMasterIP = InetAddress.getByName(IP);
                autoDiscoverMaster = false;
                isValid = true;

            } catch (UnknownHostException uhex) {
                log.info("Please enter a valid Master IP address.");
                System.out.print(commandLineTemplate);
                System.exit(0);
            }
        }
        return isValid;
    }

    /**
     * Loki command line usage
     * java -jar LokiRender-<ver>.jar [<BlenderExe>] [<MasterIP>]
     *
     * java -jar LokiRender-071.jar
     * java -jar LokiRender-071.jar /path/to/blender
     * java -jar LokiRender-071.jar 192.168.17.45
     * java -jar LokiRender-071.jar /path/to/blender 192.168.17.45
     *
     * blenderExe - null, if was not found args.
     * In the same time, if ip or blender exe are not valid - exits from JVM
     * @param args
     */
    private static void handleCommandLineArgs(String[] args) {

        switch (args.length) {
            case 0:
                break;
            case 1:
                String firstArg = args[0];

                if (isValidIP(firstArg)) {
                    // do nothing
                } else if (CommandLineHelper.isValidBlenderExe(firstArg)) {
                    blenderExe = firstArg;
                    isGruntFromCommandLine = true;
                } else {
                    log.info("invalid argument");
                    System.out.print(commandLineTemplate);
                    System.exit(1);
                }
                break;
            case 2:
                String blenderPath = args[0];
                String inputIP = args[1];

                if (CommandLineHelper.isValidBlenderExe(blenderPath)) {
                    blenderExe = blenderPath;
                    isValidIP(inputIP);
                    isGruntFromCommandLine = true;
                } else {
                    log.info("invalid blender executable");
                    System.exit(1);
                }
                break;
            default:
                System.out.print(commandLineTemplate);
                System.exit(0);
        }
    }

    private static String getCommandLineTemplate() {
        return "\nUsage: java -jar LokiRender-<ver>.jar [<BlenderExe>] [<MasterIP>]\n\n" +
                "Examples:\n" +
                "java -jar LokiRender-071.jar\n" +
                "java -jar LokiRender-071.jar /path/to/blender\n" +
                "java -jar LokiRender-071.jar 192.168.17.45\n" +
                "java -jar LokiRender-071.jar /path/to/blender 192.168.17.45\n\n" +
                "Loki will start in grunt command line mode (no GUI) if\n" +
                "a blender executable is provided as an argument.\n\n";
    }
}
