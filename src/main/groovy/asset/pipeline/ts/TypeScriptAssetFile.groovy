package asset.pipeline.ts

import asset.pipeline.AbstractAssetFile

import java.util.regex.Pattern

class TypeScriptAssetFile extends AbstractAssetFile {

    static final contentType = ['application/javascript', 'application/x-javascript', 'text/javascript']
    static extensions = ['ts']
    static final String compiledExtension = 'js'
    static processors = [TypeScriptProcessor]
    Pattern directivePattern = ~/(?m)#=(.*)/

}
