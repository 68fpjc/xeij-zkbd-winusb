#----------------------------------------------------------------

# Java compiler
ifeq ("$(JAVA_HOME)","")
JAVA_HOME = $(shell readlink -f `which javac` | sed "s:/bin/javac[^/]*$$::")
endif
JAR = $(JAVA_HOME)/bin/jar
JAVA = $(JAVA_HOME)/bin/java
JAVAC = $(JAVA_HOME)/bin/javac
JAVA_MINIMUM_REQUIRED = 26

# classpath separator
ifeq ("$(OS)","Windows_NT")
CP_SEPARATOR = ;
else
CP_SEPARATOR = :
endif

# system property
SYSTEM_PROPERTY = -Dsun.java2d.d3d=false -Dsun.java2d.xrender=false

#----------------------------------------------------------------

# program name
PROGRAM = XEiJ
LOWER_PROGRAM = xeij

# manifest file name
MANIFEST = manifest.txt

# package name
MAIN_PACKAGE = xeij

# main class name
MAIN_CLASS = XEiJ

# class directory name
CLASS_DIR = class

# data directory name
DATA_DIR = data

# jar file names of external libraries
LIBRARY_JAR_1 = jSerialComm-2.11.4.jar

# representative file names
REP_CLASS_FILE = $(CLASS_DIR)/$(MAIN_PACKAGE)/$(MAIN_CLASS).class

# obsolete files
OBSOLETE_FILE_1 = $(MAIN_PACKAGE)/SerialPort.java
OBSOLETE_FILE_2 = $(MAIN_PACKAGE)/OldSerialPort.java
OBSOLETE_FILE_3 = $(MAIN_PACKAGE)/WinDLL.java

#----------------------------------------------------------------

ifeq ("$(BOOT)","")
BOOT = misc
endif

all:
	@$(MAKE) --no-print-directory $(PROGRAM).jar

test: all
	$(JAVA) $(SYSTEM_PROPERTY) -jar $(PROGRAM).jar -boot=$(BOOT) $(PARAM)

#----------------------------------------------------------------

$(PROGRAM).jar: $(MANIFEST) $(REP_CLASS_FILE) $(DATA_DIR)/*
	-rm $@
	$(JAR) cfm $@ $(MANIFEST) -C $(CLASS_DIR) . -C . $(DATA_DIR)/*

$(MANIFEST): Makefile
	echo "Manifest-Version: 1.0" > $@
	echo "Class-Path: . $(LIBRARY_JAR_1)" >> $@
	echo "Main-Class: $(MAIN_PACKAGE)/$(MAIN_CLASS)" >> $@
	echo "Enable-Native-Access: ALL-UNNAMED" >> $@

$(REP_CLASS_FILE): $(MAIN_PACKAGE)/*.java
	@if [ -e $(OBSOLETE_FILE_1) ]; then \
		echo "Obsolete file $(OBSOLETE_FILE_1) should be removed"; \
		exit 1; \
	fi
	@if [ -e $(OBSOLETE_FILE_2) ]; then \
		echo "Obsolete file $(OBSOLETE_FILE_2) should be removed"; \
		exit 1; \
	fi
	@if [ -e $(OBSOLETE_FILE_3) ]; then \
		echo "Obsolete file $(OBSOLETE_FILE_3) should be removed"; \
		exit 1; \
	fi
	-rm -r $(CLASS_DIR)
	-mkdir -p $(CLASS_DIR)/$(MAIN_PACKAGE)
	$(JAVAC) --release $(JAVA_MINIMUM_REQUIRED) \
		-encoding UTF-8 -cp "$(MAIN_PACKAGE)$(CP_SEPARATOR)$(LIBRARY_JAR_1)" \
		-d $(CLASS_DIR) $(MAIN_PACKAGE)/*.java -Xlint:all,-serial -Xdiags:verbose

#----------------------------------------------------------------

clean:
	-rm $(PROGRAM).jar
	-rm $(MANIFEST)
	-rm -r $(CLASS_DIR)

#----------------------------------------------------------------
