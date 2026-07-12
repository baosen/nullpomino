"""Bazel rule for compiling JVM bytecode to JavaScript with TeaVM."""

def _teavm_js_impl(ctx):
    classpath = depset(transitive = [
        target[DefaultInfo].files
        for target in ctx.attr.classpath
    ]).to_list()

    args = ctx.actions.args()
    args.add(ctx.outputs.out.path)
    args.add(ctx.attr.main_class)
    args.add(ctx.attr.optimization)
    args.add("true" if ctx.attr.strict else "false")
    args.add("true" if ctx.attr.obfuscated else "false")
    args.add_all(classpath)

    ctx.actions.run(
        executable = ctx.executable._compiler,
        arguments = [args],
        inputs = classpath,
        outputs = [ctx.outputs.out],
        tools = [ctx.attr._compiler[DefaultInfo].files_to_run],
        mnemonic = "TeaVMCompile",
        progress_message = "Compiling %{label} with TeaVM",
    )

    return [DefaultInfo(files = depset([ctx.outputs.out]))]

teavm_js = rule(
    implementation = _teavm_js_impl,
    attrs = {
        "classpath": attr.label_list(
            allow_files = [".jar"],
            mandatory = True,
        ),
        "main_class": attr.string(mandatory = True),
        "obfuscated": attr.bool(default = False),
        "optimization": attr.string(
            default = "SIMPLE",
            values = ["SIMPLE", "ADVANCED", "FULL"],
        ),
        "out": attr.output(mandatory = True),
        "strict": attr.bool(default = True),
        "_compiler": attr.label(
            default = Label("//web:teavm_compiler"),
            cfg = "exec",
            executable = True,
        ),
    },
)
