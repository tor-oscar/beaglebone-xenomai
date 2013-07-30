% Installing xenomai and debian on the beaglebone/black
% Sagar Behere
% 30 July 2013

This document presents one method to get a working linux 3.8.13 kernel patched with xenomai-2.6, together with a debian arm root filesystem.

This method is illustrated for an armhf rootfs, but can just as easily be applied to an armel rootfs. To do so, replace all occurrences of *arm-linux-gnueabihf-* with *arm-linux-gnueabi-*

This document is split into two parts. 

1. The first part shows how to compile the kernel and xenomai. 
2. The second part shows how to incorporate the compiled kernel+kernel modules+firmware into the rootfs. Those who do not wish to use a debian rootfs may skip the second part.

The method presented in this document has been tested on a debian 7.0 (Wheezy) amd64 host and uses toolchains and software repositories from the [emdebian](http://emdebian.org) and [debian](http://www.debian.org) projects.

This document is written by Sagar Behere, sagar.behere@gmail.com based on an earlier document by Oscar Olsson, osse.olsson@gmail.com. The original instructions reside in the README.md file at <https://github.com/DrunkenInfant/beaglebone-xenomai>

# 1. Compiling the kernel and xenomai

## 1.1 Install the cross-compiler toolchain

First, you need to install the arm crosstoolchain of your choice. The available types are armel and armhf. Google has plenty of information about their differences; what is important at this point is to know that an armel toolchain will produce armel binaries and these will **NOT** work on an armhf rootfs and *vice-versa*. There is no easy way to determine whether a running system is armhf or armel (but see <https://blogs.oracle.com/jtc/entry/is_it_armhf_or_armel>). That said, armel/armhf makes no difference for the kernel itself. It is the userspace programs/libraries that the differences apply to. Either kernel will boot fine with either userland.

We will install the toolchain provided from the [emdebian](http://emdebian.org) project. We need to add the project's repository to the host system.

(Reference: <http://wiki.debian.org/EmdebianToolchain>)

	apt-get install emdebian-archive-keyring
	cat "deb http://www.emdebian.org/debian unstable main" > /etc/apt/sources.list.d/emdebian.sources.list
	apt-get update

Note that we are using the emdebian unstable repository, which is not really recommended. We do this because the stable repository does not have the armhf toolchain (yet). If you intend to use the armel toolchain, the stable repository may be better suited to your needs.

Next, install the toolchain itself:

	apt-get install g++-4.7-arm-linux-gnueabihf gcc-4.7-arm-linux-gnueabihf

Use whichever version you require. You may have to make the following symlinks manually

	ln -s /usr/bin/arm-linux-gnueabihf-gcc-4.7 /usr/bin/arm-linux-gnueabi-gcc
	ln -s /usr/bin/arm-linux-gnueabihf-g++-4.7 /usr/bin/arm-linux-gnueabi-g++


## 1.2 Obtain and prepare all sources

In this section, we will clone various repositories and checkout their correct branches/commits. Then we'll apply patches to and from various software sources. For the purpose of this document, all the repositories will be cloned into $HOME/bbb and all subsequent work will be done in this directory.

	mkdir ~/bbb
	cd ~/bbb

### 1.2.1 kernel sources

We will use the kernel sources from the xenomai project's [ipipe-jki](http://git.xenomai.org/ipipe-jki.git/) git repository. These sources already have the ipipe patches, so we do not need to hunt for and apply ipipe patches specific to the particular kernel version we are using. We will be using the branch corresponding to the 3.8.13 kernel, which is called for-upstream/3.8

	git clone git://git.xenomai.org/ipipe-jki.git
	cd ipipe-jki; 
	git checkout -b 3.8 origin/for-upstream/3.8

### 1.2.2 xenomai sources

	git clone git://git.xenomai.org/xenomai-2.6.git

### 1.2.3 beaglebone-xenomai scripts

[This repo](https://github.com/DrunkenInfant/beaglebone-xenomai) contains some scripts and recipes for applying beaglebone specific patches (see next sub-section) to the kernel sources

	git clone https://github.com/DrunkenInfant/beaglebone-xenomai

### 1.2.4 meta-beagleboard patches

We need to apply some beaglebone specific patches to the kernel sources. These patches are in the [meta beagleboard](https://github.com/beagleboard/meta-beagleboard.git) repository. 

	git clone https://github.com/beagleboard/meta-beagleboard.git
	cd meta-beagleboard

We need to check out the particular commit corresponding to kernel 3.8.13. We can find this commit by examining the output of the command

	git log -S3.8.13 --source --all

which indicates that commit with hash 50316366dd4f75027ee5291b65a9bbcfa9a9e840 is the one we need.

	git checkout 50316366dd4f75027ee5291b65a9bbcfa9a9e840

(*NOTE: This process of rolling back to the particular committ may not be necessary. However in my (Sagar's) case, not rolling back resulted in many errors while applying the patches from the repository and I was too lazy to debug them all :P*)

However, all patches from this repository do not apply cleanly due to some (very minor) issues. Applying the meta-beaglebone-xenomai.patch from the beaglebone-xenomai repository (see previous sub-section) fixes this

	patch -p1 < ~/bbb/beaglebone-xenomai/meta-beaglebone-xenomai.patch

(*NOTE: the meta-beaglebone-xenomai.patch contains patches for three files, one of which was missing from my meta-beagleboard repository as prepared so far. So I (Sagar) simply did not apply the patch for this file.*)

### 1.2.5 Apply meta-beagleboard patches to kernel sources

Open the file ~/bbb/beaglebone-xenomai/apply-beaglebone-patches.sh and change the META_BEAGLEBONE_ROOT to the location of your cloned version of the meta-beaglebone repo. If you have followed the steps above, the relevant line should look like

	META_BEAGLEBONE_ROOT=~/bbb/meta-beagleboard

Verify that the PATCHSETS variable in the same file does not contain any directories that are not present in the meta-beagleboard's common-bsp/recipes-kernel/linux/linux-mainline-3.8 directory. If the PATCHSETS variable has extra directories, remove them.

(*NOTE: According to the author of the beaglebone-xenomai repo, Oscar Olsson, the PATCHSETS variable indeed contains more directories than those in meta-beagleboard's common-bsp/recipes-kernel/linux/linux-mainline-3.8 directory. However, in my (Sagar's) experience, this is not the case. Every directory in PATCHSETS was present under meta-beagleboard's common-bsp/recipes-kernel/linux/linux-mainline-3.8 directory.*)

	cd ~/bbb/ipipe-jki
	source ~/bbb/beaglebone-xenomai/apply-beaglebone-patches.sh

These patches SHOULD apply cleanly. If there are any errors at this step, resolve them before continuing.

### 1.2.6 Prepare the kernel sources for xenomai

	cd ~/bbb/ipipe-jki
	~/bbb/xenomai-2.6/scripts/prepare-kernel.sh --arch=arm

(*NOTE/QUESTION: Why is this step necessary? Doesn't prepare-kernel.sh simply apply xenomai patches to the kernel sources.. and our kernel sources don't need that because they are taken directly from the ipipe-jki repository?*)

## 1.3 Compile the kernel

You may wish to edit the ~/bbb/ipipe-jki/Makefile and add a string of your choice to EXTRAVERSION. I (Sagar) tend to use "-xenomai-2.6" (without the quotes).

We start off with a known, good kernel config

	cd ~/bbb/ipipe-jki
	cp ~/bbb/meta-beagleboard/common-bsp/recipes-kernel/linux/linux-mainline-3.8/beaglebone/defconfig arch/arm/configs/

Then load that config while doing the menuconfig

	make ARCH=arm menuconfig

Configure the options for your specific needs, but perform at least the following steps


1. Make sure the submenu 'Real-time sub-system' exists, if not the prepare-kernel script was not successful.
2. Disable CPU frequency scaling, 'CPU Power Management ---> CPU Frequency scaling'
3. Check the Real-time sub-system menu. If there are any conflicts left there will be a warning in that menu.

Next, download the power management firmware from <http://arago-project.org/git/projects/?p=am33x-cm3.git;a=blob;f=bin/am335x-pm-firmware.bin;h=571d377dc50cc7bb8258facec8948b86b8025248;hb=cf07b841d6e8c5e026eecb259d143f3dff412c8e> and copy it to ~/bbb/ipipe-jki/firmware/am335x-pm-firmware.bin

Now compile the kernel with

	make ARCH=arm CROSS_COMPILE=arm-linux-gnueabihf- ZRELADDR=0x80008000 uImage modules

After successful compilation, the newly compiled kernel will be found in various formats (uImage/zImage/...) under arch/arm/boot/

This appropriate kernel file should be copied to the appropriate location in your system image (SD card). The kernel modules and firmware should also be copied into the proper location of the rootfs. Part 2 of this document shows how to do this for a debian wheezy armhf system, but for the sake of completeness, know that the modules/firmware can be installed with the commands

	make ARCH=arm CROSS_COMPILE=arm-linux-gnueabihf- modules_install INSTALL_MOD_PATH=/path/to/rootfs/
	make ARCH=arm CROSS_COMPILE=arm-linux-gnueabihf- firmware_install INSTALL_FW_PATH=/path/to/rootfs/

## 1.4 Compile xenomai userspace

	cd ~/bbb/xenomai-2.6
	./configure --host=arm-linux-gnueabihf
	make

The installation to the rootfs mounted on the host is done by

	make DESTDIR=/path/to/mounted/rootfs install

(*NOTE: The original guide gives the command for this as `mkdir staging && make DESTDIR=/path/to/mounted/rootfs install` but I (Sagar) have not understood why the 'mkdir staging &&' part is necessary, or if it in fact does anything useful.*)

# 2 Preparing the SD Card with rootfs

This part shows how to create a debian rootfs.

Reference: <http://elinux.org/BeagleBoardDebian>

	git clone git://github.com/RobertCNelson/netinstall.git
	sudo ./mk_mmc.sh --mmc /dev/sdX --dtb am335x-boneblack --distro wheezy-armhf --firmware

Make sure to substitute /dev/sdX in the command above with the correct device for your sd card.

Alternatively, you can download and use the ready image as described at <http://elinux.org/BeagleBoardDebian#Demo_Image>

Now we need to copy the newly compiled kernel to this sd card.

	mount /dev/sdX1 /mnt
	cp /mnt/zImage /mnt/zImage-original
	cp ~/bbb/ipipe-jki/arch/arm/boot/zImage /mnt/
	sync
	umount /mnt

Finally, copy the kernel modules and firmware to the rootfs

	mount /dev/sdX2 /mnt
	cd ~/bbb/ipipe-jki
	make ARCH=arm CROSS_COMPILE=arm-linux-gnueabihf- modules_install INSTALL_MOD_PATH=/mnt/
	make ARCH=arm CROSS_COMPILE=arm-linux-gnueabihf- firmware_install INSTALL_FW_PATH=/mnt/
	sync
	umount /mnt


And that's it! Insert the sd card into your beaglebone/black and it should boot with the xenomai kernel 3.8.13 and debian wheezy userland.

Once the board boots, you may find it necessary to perform the following

	addgroup xenomai
	echo '/usr/xenomai/lib' >> /etc/ld.so.conf
	ldconfig

Test whether xenomai works by doing, for example, the latency test

	cd /usr/xenomai/bin
	./latency -p0 -t1
