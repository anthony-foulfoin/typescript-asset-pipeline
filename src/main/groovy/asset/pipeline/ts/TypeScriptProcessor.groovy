package asset.pipeline.ts

import asset.pipeline.*
import groovy.io.FileType
import groovy.transform.CompileStatic
import org.slf4j.Logger
import org.slf4j.LoggerFactory

class TypeScriptProcessor extends AbstractProcessor {

    static Logger log = LoggerFactory.getLogger(TypeScriptProcessor.class)

    static boolean FIRST_COMPILATION = true
    protected static final String TYPESCRIPT_PART = 'typescript' + File.separator

    TypeScriptProcessor(AssetCompiler precompiler) {
        super(precompiler)
    }

    @Override
    String process(String inputText, AssetFile assetFile) {

        String filePath = assetFile.sourceResolver.baseDirectory.path + '/' + assetFile.path
        File originalFile = new File(filePath)

        //Double lock :
        // On sait qu'il y a 5 compilation en parallèle
        // La première fois ils passent tous dans le premier if
        // puis le premier compile tout et passe la variable a false
        // et les 4 autres n'y passent plus
        // les fois suivants, la variable est false, et le synchronized n'est plus jamais "utilisé"
        if (FIRST_COMPILATION) {
            synchronized (TypeScriptProcessor) {
                if (FIRST_COMPILATION) {
                    compileAllFilesInOneRun(originalFile)
                    FIRST_COMPILATION = false
                }


            }
            def md5 = AssetHelper.getByteDigest(originalFile.bytes)
            // Le fichier est censé etre en cache
            def result = CacheManager.findCache(originalFile.canonicalPath, md5, null)
            log.info "Récupération de $originalFile.path depuis le cache"

            result ?: compileFile(originalFile)
        } else {
            compileFile(originalFile)
        }
    }

    @CompileStatic
    private String compileFile(File originalFile) {
        String originalFilePath = originalFile.path

        if (originalFilePath.endsWith('.d.ts')) {
            return ''
        }
        log.info "Compilation de $originalFilePath"
        String newFilePath = originalFilePath.replaceAll('\\.ts', '.js')

        executeTsc(originalFilePath, newFilePath)

        getContentThenDelete(newFilePath)
    }

    //Optimisation : il est beaucoup plus rapide de compiler plusieurs fichier TypeScript en meme temps
    //Donc à la première compilation, on compile tout d'un coup
    //Ensuite on les compilera un par un
    //C'est globalement un "hack" de l'asset pipeline car ca s'appuie sur le fait que le pipeline
    //Enregistre en cache les fichiers générés
    @CompileStatic
    private void compileAllFilesInOneRun(File originalFile) {
        Date start = new Date()

        String originalFileDirectory = originalFile.parent

        Integer indexOfValinorPart = originalFileDirectory.indexOf(TYPESCRIPT_PART)
        if (indexOfValinorPart >= 0) {
            String originalDirectory = originalFileDirectory.substring(0, indexOfValinorPart + TYPESCRIPT_PART.size())

            List<File> allFiles = findAllTypeScriptFiles(originalDirectory)

            log.info 'Compilation de tous les fichiers TypeScript'

            executeTsc(allFiles)

            allFiles.each { File oldFile ->
                String newFilePath = oldFile.path.replace('.ts', '.js')

                String fileText = getContentThenDelete(newFilePath)

                def md5 = AssetHelper.getByteDigest(oldFile.text.bytes)

                def path = oldFile.canonicalPath.substring(oldFile.canonicalPath.indexOf(TYPESCRIPT_PART) + TYPESCRIPT_PART.size())
                CacheManager.createCache(path, md5, fileText, null)
            }

            log.info "Compilation des fichiers TypeScript terminé en ${start.time - new Date().time} ms)}"
        }
    }

    @CompileStatic
    private String getContentThenDelete(String newFilePath) {
        File tmpTsFile = new File(newFilePath)
        String fileText = tmpTsFile.text

        if (!tmpTsFile.delete()) {
            throw new IOException("$newFilePath can not be deleted")
        }

        fileText
    }

    @CompileStatic
    private boolean executeTsc(String file, String toFile) {
        String tscExecutable = findTscPath()

        String cmd = "$tscExecutable --out $toFile -t ES5  $file"
        executeTsc(cmd)
    }

    @CompileStatic
    private boolean executeTsc(List<File> files) {
        String tscExecutable = findTscPath()
        String file = files*.path.join(' ')

        String cmd = "$tscExecutable -t ES5 $file"

        executeTsc(cmd)
    }

    @CompileStatic
    private boolean executeTsc(String cmd) {
        Process proc = cmd.execute()
        proc.waitFor()

        String tscError = proc.err.text
        String tscOutput = proc.in.text

        if (tscOutput) {
            log.info "typescript out : $tscOutput"
        }
        if (tscError) {
            log.warn "typescript error: $tscError"
        }

        !tscError
    }

    @CompileStatic
    private String findTscPath() {
        String tscExecutable = './node_modules/.bin/tsc'

        // If the OS is windows we must use the .cmd and not the binary directly
        if (System.getProperty('os.name').toLowerCase().contains('win')) {
            tscExecutable = './node_modules/.bin/tsc.cmd'
        }

        File tscExecutableFile = new File(tscExecutable)

        if (tscExecutableFile.exists()) {
            tscExecutable = tscExecutableFile.getAbsolutePath()
        } else {
            log.debug "Utilisation de l'install globale de tsc"
            tscExecutable = 'tsc'
        }
        tscExecutable
    }

    @CompileStatic
    private List<File> findAllTypeScriptFiles(String originalDirectory) {
        List<File> tsFiles = []

        new File(originalDirectory).eachFileRecurse(FileType.FILES) { File file ->
            if (file.name.endsWith('.ts') && !file.name.endsWith('.d.ts')) {
                tsFiles << file
            }
        }

        tsFiles

    }
}
