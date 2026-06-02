# MultiAlarms build — plain javac + jar, no external build tool required.
#
# Targets:
#   make            build dist/MultiAlarms.jar (default)
#   make run        build and launch the GUI
#   make clean      remove build output

JAVAC      := javac
JAR        := jar
JAVA       := java

SRC_DIR    := src
RES_DIR    := res
LIB_DIR    := lib
BUILD_DIR  := build
CLASSES    := $(BUILD_DIR)/classes
PKG_DIR    := $(CLASSES)/multialarms
DIST_DIR   := dist
OUT_JAR    := $(DIST_DIR)/MultiAlarms.jar

KUNSTSTOFF := $(LIB_DIR)/kunststoff.jar
SOURCES    := $(shell find $(SRC_DIR) -name '*.java')

# Resources to drop alongside the .class files so getResource("alarm.au") etc. resolve.
RES_NAMES  := alarm.au bell.gif bell_white.gif splash.png
RES_DST    := $(addprefix $(PKG_DIR)/,$(RES_NAMES)) $(PKG_DIR)/multialarms.properties

# --release 8 sets source + target + bootclasspath in one shot; matches the
# original 1.6/1.8-era code without producing the "bootstrap class path not
# set" warning that -source/-target alone would.
JAVAC_FLAGS := -encoding UTF-8 --release 8

.PHONY: all jar compile clean run

all: jar

# --- compile ---------------------------------------------------------------

compile: $(CLASSES)/.compiled

$(CLASSES)/.compiled: $(SOURCES) $(KUNSTSTOFF)
	@mkdir -p $(CLASSES)
	$(JAVAC) $(JAVAC_FLAGS) -cp $(KUNSTSTOFF) -d $(CLASSES) $(SOURCES)
	@touch $@

# --- resources -------------------------------------------------------------

$(PKG_DIR)/%: $(RES_DIR)/%
	@mkdir -p $(PKG_DIR)
	cp $< $@

$(PKG_DIR)/multialarms.properties: $(SRC_DIR)/multialarms/multialarms.properties
	@mkdir -p $(PKG_DIR)
	cp $< $@

# --- jar -------------------------------------------------------------------
# Unpack the kunststoff lib into the classes dir so the result is a single
# runnable jar (matches the original dist/MultiAlarms.jar layout).

jar: $(OUT_JAR)

$(OUT_JAR): $(CLASSES)/.compiled $(RES_DST)
	@mkdir -p $(DIST_DIR)
	cd $(CLASSES) && $(JAR) xf $(abspath $(KUNSTSTOFF))
	rm -rf $(CLASSES)/META-INF
	$(JAR) cfe $@ multialarms.MultiAlarms -C $(CLASSES) .

# --- run / clean -----------------------------------------------------------

run: jar
	$(JAVA) -jar $(OUT_JAR)

clean:
	rm -rf $(BUILD_DIR)
