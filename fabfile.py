#-*- coding: utf-8 -*-

from fabric.api import *

@task
def release(version, new_version):
    sed_version(version)
    commit("v%s" % version)
    add_tag("v%s" % version)
    sed_version("%s-SNAPSHOT" % new_version)
    commit("v%s-SNAPSHOT" % new_version)
    push()
    push_tags()

def push_tags():
    local("""
git push --tags
    """)

def push():
    local("""
git push origin master
    """)

def add_tag(comment):
    local("""
git tag -a %(comment)s -m %(comment)s
    """ % {
        "comment": comment,
    })

def commit(comment):
    local("""
git commit -a -m '%s'
    """ % comment)

def sed_version(version):
    local("""
sed -i '' -e 's/".*"/"%s"/g' version.sbt
    """ % version)

