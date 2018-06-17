/**
 * Common checkout module
 */

if( CFG.'git.url' )
  MPLModule('Git Checkout', CFG)
else
  MPLModule('Default Checkout', CFG)
