DESCRIPTION = "Xenomai libraries"
SECTION = "kernel"
LICENSE = "GPLv2"
HOMEPAGE = "http://xenomai.org/"
DEPENDS = ""
LIC_FILES_CHKSUM = "file://CREDITS;md5=7845f4d8e94ed36651ecff554dd2b0be"

S = "${WORKDIR}/xenomai-${PV}"

PR = "r0"

SRC_URI += "http://download.gna.org/xenomai/stable/xenomai-${PV}.tar.bz2 \
           "
SRC_URI[md5sum] = "3dca1eca040486f8f165f5e340c7a25e"
#SRC_URI = "git://git.xenomai.org/xenomai-2.6.git;protocol=git;tag=v${PV}"

do_configure () {
	./configure CFLAGS="-march=armv7-a" \
		LDFLAGS="-march=armv7-a" \
		--disable-doc-install \
		--enable-debug=no \
		--host=arm-linux-gnueabi
}

do_compile() {
	oe_runmake
}

do_install() {
	oe_runmake DESTDIR=${D} install
}

PACKAGES = "${PN}-dbg ${PN}-staticdev ${PN}-dev"
# We enumerate all directories excluding /usr/xenomai/share/ (it contains only 
# documentation which occupies about 33MB)
FILES_${PN}-dbg = "\
/usr/src/debug/beaglebone-xenomai-userspace-2.6.2.1-r0 \
/usr/xenomai/bin/.debug \
/usr/xenomai/bin/regression/native/.debug \
/usr/xenomai/bin/regression/posix/.debug \
/usr/xenomai/bin/regression/native+posix/.debug \
/usr/xenomai/lib/.debug \
/usr/xenomai/sbin/.debug"
FILES_${PN}-staticdev = "\
/usr/xenomai/lib/*.a"
FILES_${PN}-dev = "\
/dev/rtp* \
/dev/rtheap \
/usr/xenomai/bin \
/usr/xenomai/include \
/usr/xenomai/lib \
/usr/xenomai/sbin"

